package eu.kanade.tachiyomi.data.library

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Matchers.anyLong
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import java.util.*

@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP))
@RunWith(CustomRobolectricGradleTestRunner::class)
class LibraryUpdateServiceTest {

    lateinit var app: Application
    lateinit var context: Context
    lateinit var service: LibraryUpdateService
    lateinit var source: HttpSource

    @Before
    fun setup() {
        app = RuntimeEnvironment.application
        context = app.applicationContext

        // Mock the source manager
        val module = object : InjektModule {
            override fun InjektRegistrar.registerInjectables() {
                addSingleton(Mockito.mock(SourceManager::class.java, RETURNS_DEEP_STUBS))
            }
        }
        Injekt.importModule(module)

        service = Robolectric.setupService(LibraryUpdateService::class.java)
        source = mock(HttpSource::class.java)
        `when`(service.sourceManager.get(anyLong())).thenReturn(source)
    }

    @Test
    fun testLifecycle() {
        // Smoke test
        Robolectric.buildService(LibraryUpdateService::class.java)
                .attach()
                .create()
                .startCommand(0, 0)
                .destroy()
                .get()
    }

    @Test
    fun testUpdateManga() {
        val manga = createManga("/manga1")[0]
        manga.id = 1L
        service.db.insertManga(manga).executeAsBlocking()

        val sourceChapters = createChapters("/chapter1", "/chapter2")

        `when`(source.fetchChapterList(manga)).thenReturn(Observable.just(sourceChapters))

        service.updateManga(manga).subscribe()

        assertThat(service.db.getChapters(manga).executeAsBlocking()).hasSize(2)
    }

    @Test
    fun testContinuesUpdatingWhenAMangaFails() {
        var favManga = createManga("/manga1", "/manga2", "/manga3")
        service.db.insertMangas(favManga).executeAsBlocking()
        favManga = service.db.getFavoriteMangas().executeAsBlocking()

        val chapters = createChapters("/chapter1", "/chapter2")
        val chapters3 = createChapters("/achapter1", "/achapter2")

        // One of the updates will fail
        `when`(source.fetchChapterList(favManga[0])).thenReturn(Observable.just(chapters))
        `when`(source.fetchChapterList(favManga[1])).thenReturn(Observable.error<List<SChapter>>(Exception()))
        `when`(source.fetchChapterList(favManga[2])).thenReturn(Observable.just(chapters3))

        val intent = Intent()
        service.updateChapterList(service.getMangaToUpdate(intent)).subscribe()

        // There are 3 network attempts and 2 insertions (1 request failed)
        assertThat(service.db.getChapters(favManga[0]).executeAsBlocking()).hasSize(2)
        assertThat(service.db.getChapters(favManga[1]).executeAsBlocking()).hasSize(0)
        assertThat(service.db.getChapters(favManga[2]).executeAsBlocking()).hasSize(2)
    }

    private fun createChapters(vararg urls: String): List<Chapter> {
        val list = ArrayList<Chapter>()
        for (url in urls) {
            val c = Chapter.create()
            c.url = url
            c.name = url.substring(1)
            list.add(c)
        }
        return list
    }

    private fun createManga(vararg urls: String): List<Manga> {
        val list = ArrayList<Manga>()
        for (url in urls) {
            val m = Manga.create(url, url.substring(1))
            m.favorite = true
            list.add(m)
        }
        return list
    }
}
