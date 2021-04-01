package eu.kanade.tachiyomi.data.database

import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner
import eu.kanade.tachiyomi.data.database.models.CategoryImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(constants = BuildConfig::class, sdk = [Build.VERSION_CODES.M])
@RunWith(CustomRobolectricGradleTestRunner::class)
class CategoryTest {

    lateinit var db: DatabaseHelper

    @Before
    fun setup() {
        val app = RuntimeEnvironment.application
        db = DatabaseHelper(app)

        // Create 5 manga
        createManga("a")
        createManga("b")
        createManga("c")
        createManga("d")
        createManga("e")
    }

    @Test
    fun testHasCategories() {
        // Create 2 categories
        createCategory("Reading")
        createCategory("Hold")

        val categories = db.getCategories().executeAsBlocking()
        assertThat(categories).hasSize(2)
    }

    @Test
    fun testHasLibraryMangas() {
        val mangas = db.getLibraryMangas().executeAsBlocking()
        assertThat(mangas).hasSize(5)
    }

    @Test
    fun testHasCorrectFavorites() {
        val m = Manga.create(0)
        m.title = "title"
        m.author = ""
        m.artist = ""
        m.thumbnail_url = ""
        m.genre = "a list of genres"
        m.description = "long description"
        m.url = "url to manga"
        m.favorite = false
        db.insertManga(m).executeAsBlocking()
        val mangas = db.getLibraryMangas().executeAsBlocking()
        assertThat(mangas).hasSize(5)
    }

    @Test
    fun testMangaInCategory() {
        // Create 2 categories
        createCategory("Reading")
        createCategory("Hold")

        // It should not have 0 as id
        val c = db.getCategories().executeAsBlocking()[0]
        assertThat(c.id).isNotZero

        // Add a manga to a category
        val m = db.getMangas().executeAsBlocking()[0]
        val mc = MangaCategory.create(m, c)
        db.insertMangaCategory(mc).executeAsBlocking()

        // Get mangas from library and assert manga category is the same
        val mangas = db.getLibraryMangas().executeAsBlocking()
        for (manga in mangas) {
            if (manga.id == m.id) {
                assertThat(manga.category).isEqualTo(c.id)
            }
        }
    }

    private fun createManga(title: String) {
        val m = Manga.create(0)
        m.title = title
        m.author = ""
        m.artist = ""
        m.thumbnail_url = ""
        m.genre = "a list of genres"
        m.description = "long description"
        m.url = "url to manga"
        m.favorite = true
        db.insertManga(m).executeAsBlocking()
    }

    private fun createCategory(name: String) {
        val c = CategoryImpl()
        c.name = name
        db.insertCategory(c).executeAsBlocking()
    }
}
