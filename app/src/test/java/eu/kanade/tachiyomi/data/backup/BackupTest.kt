package eu.kanade.tachiyomi.data.backup

import android.app.Application
import android.content.Context
import android.os.Build
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner
import eu.kanade.tachiyomi.data.backup.legacy.LegacyBackupManager
import eu.kanade.tachiyomi.data.backup.legacy.models.Backup
import eu.kanade.tachiyomi.data.backup.legacy.models.DHistory
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Category
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.ChapterImpl
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.data.database.models.MangaImpl
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.database.models.TrackImpl
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.setting.OrientationType
import eu.kanade.tachiyomi.ui.reader.setting.ReadingModeType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.mock
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton

/**
 * Test class for the [LegacyBackupManager].
 * Note that this does not include the backup create/restore services.
 */
@Config(constants = BuildConfig::class, sdk = [Build.VERSION_CODES.M])
@RunWith(CustomRobolectricGradleTestRunner::class)
class BackupTest {
    // Create root object
    var root = Backup()

    // Create information object
    var information = buildJsonObject {}

    lateinit var app: Application
    lateinit var context: Context
    lateinit var source: HttpSource

    lateinit var legacyBackupManager: LegacyBackupManager

    lateinit var db: DatabaseHelper

    @Before
    fun setup() {
        app = RuntimeEnvironment.application
        context = app.applicationContext
        legacyBackupManager = LegacyBackupManager(context, 2)
        db = legacyBackupManager.databaseHelper

        // Mock the source manager
        val module = object : InjektModule {
            override fun InjektRegistrar.registerInjectables() {
                addSingleton(mock(SourceManager::class.java, RETURNS_DEEP_STUBS))
            }
        }
        Injekt.importModule(module)

        source = mock(HttpSource::class.java)
        `when`(legacyBackupManager.sourceManager.get(anyLong())).thenReturn(source)
    }

    /**
     * Test that checks if no crashes when no categories in library.
     */
    @Test
    fun testRestoreEmptyCategory() {
        // Restore Json
        legacyBackupManager.restoreCategories(root.categories ?: emptyList())

        // Check if empty
        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).isEmpty()
    }

    /**
     * Test to check if single category gets restored
     */
    @Test
    fun testRestoreSingleCategory() {
        // Create category and add to json
        val category = addSingleCategory("category")

        // Restore Json
        legacyBackupManager.restoreCategories(root.categories ?: emptyList())

        // Check if successful
        val dbCats = legacyBackupManager.databaseHelper.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(1)
        assertThat(dbCats[0].name).isEqualTo(category.name)
    }

    /**
     * Test to check if multiple categories get restored.
     */
    @Test
    fun testRestoreMultipleCategories() {
        // Create category and add to json
        val category = addSingleCategory("category")
        val category2 = addSingleCategory("category2")
        val category3 = addSingleCategory("category3")
        val category4 = addSingleCategory("category4")
        val category5 = addSingleCategory("category5")

        // Insert category to test if no duplicates on restore.
        db.insertCategory(category).executeAsBlocking()

        // Restore Json
        legacyBackupManager.restoreCategories(root.categories ?: emptyList())

        // Check if successful
        val dbCats = legacyBackupManager.databaseHelper.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(5)
        assertThat(dbCats[0].name).isEqualTo(category.name)
        assertThat(dbCats[1].name).isEqualTo(category2.name)
        assertThat(dbCats[2].name).isEqualTo(category3.name)
        assertThat(dbCats[3].name).isEqualTo(category4.name)
        assertThat(dbCats[4].name).isEqualTo(category5.name)
    }

    /**
     * Test if restore of manga is successful
     */
    @Test
    fun testRestoreManga() {
        // Add manga to database
        val manga = getSingleManga("One Piece")
        manga.readingModeType = ReadingModeType.VERTICAL.flagValue
        manga.orientationType = OrientationType.PORTRAIT.flagValue
        manga.id = db.insertManga(manga).executeAsBlocking().insertedId()

        var favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(1)
        assertThat(favoriteManga[0].readingModeType).isEqualTo(ReadingModeType.VERTICAL.flagValue)
        assertThat(favoriteManga[0].orientationType).isEqualTo(OrientationType.PORTRAIT.flagValue)

        // Change manga in database to default values
        val dbManga = getSingleManga("One Piece")
        dbManga.id = manga.id
        db.insertManga(dbManga).executeAsBlocking()

        favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(1)
        assertThat(favoriteManga[0].readingModeType).isEqualTo(ReadingModeType.DEFAULT.flagValue)
        assertThat(favoriteManga[0].orientationType).isEqualTo(OrientationType.DEFAULT.flagValue)

        // Restore local manga
        legacyBackupManager.restoreMangaNoFetch(manga, dbManga)

        // Test if restore successful
        favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(1)
        assertThat(favoriteManga[0].readingModeType).isEqualTo(ReadingModeType.VERTICAL.flagValue)
        assertThat(favoriteManga[0].orientationType).isEqualTo(OrientationType.PORTRAIT.flagValue)

        // Clear database to test manga fetch
        clearDatabase()

        // Test if successful
        favoriteManga = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
        assertThat(favoriteManga).hasSize(0)

        // Restore Json
        // Create JSON from manga to test parser
        val json = legacyBackupManager.parser.encodeToString(manga)
        // Restore JSON from manga to test parser
        val jsonManga = legacyBackupManager.parser.decodeFromString<Manga>(json)

        // Restore manga with fetch observable
        val networkManga = getSingleManga("One Piece")
        networkManga.description = "This is a description"
        `when`(source.fetchMangaDetails(jsonManga)).thenReturn(Observable.just(networkManga))

        runBlocking {
            legacyBackupManager.fetchManga(source, jsonManga)

            // Check if restore successful
            val dbCats = legacyBackupManager.databaseHelper.getFavoriteMangas().executeAsBlocking()
            assertThat(dbCats).hasSize(1)
            assertThat(dbCats[0].readingModeType).isEqualTo(ReadingModeType.VERTICAL.flagValue)
            assertThat(dbCats[0].orientationType).isEqualTo(OrientationType.PORTRAIT.flagValue)
            assertThat(dbCats[0].description).isEqualTo("This is a description")
        }
    }

    /**
     * Test if chapter restore is successful
     */
    @Test
    fun testRestoreChapters() {
        // Insert manga
        val manga = getSingleManga("One Piece")
        manga.id = legacyBackupManager.databaseHelper.insertManga(manga).executeAsBlocking().insertedId()

        // Create restore list
        val chapters = mutableListOf<Chapter>()
        for (i in 1..8) {
            val chapter = getSingleChapter("Chapter $i")
            chapter.read = true
            chapters.add(chapter)
        }

        // Check parser
        val chaptersJson = legacyBackupManager.parser.encodeToString(chapters)
        val restoredChapters = legacyBackupManager.parser.decodeFromString<List<Chapter>>(chaptersJson)

        // Fetch chapters from upstream
        // Create list
        val chaptersRemote = mutableListOf<Chapter>()
        (1..10).mapTo(chaptersRemote) { getSingleChapter("Chapter $it") }
        `when`(source.fetchChapterList(manga)).thenReturn(Observable.just(chaptersRemote))

        runBlocking {
            legacyBackupManager.restoreChapters(source, manga, restoredChapters)

            val dbCats = legacyBackupManager.databaseHelper.getChapters(manga).executeAsBlocking()
            assertThat(dbCats).hasSize(10)
            assertThat(dbCats[0].read).isEqualTo(true)
        }
    }

    /**
     * Test to check if history restore works
     */
    @Test
    fun restoreHistoryForManga() {
        val manga = getSingleManga("One Piece")
        manga.id = legacyBackupManager.databaseHelper.insertManga(manga).executeAsBlocking().insertedId()

        // Create chapter
        val chapter = getSingleChapter("Chapter 1")
        chapter.manga_id = manga.id
        chapter.read = true
        chapter.id = legacyBackupManager.databaseHelper.insertChapter(chapter).executeAsBlocking().insertedId()

        val historyJson = getSingleHistory(chapter)

        val historyList = mutableListOf<DHistory>()
        historyList.add(historyJson)

        // Check parser
        val historyListJson = legacyBackupManager.parser.encodeToString(historyList)
        val history = legacyBackupManager.parser.decodeFromString<List<DHistory>>(historyListJson)

        // Restore categories
        legacyBackupManager.restoreHistoryForManga(history)

        val historyDB = legacyBackupManager.databaseHelper.getHistoryByMangaId(manga.id!!).executeAsBlocking()
        assertThat(historyDB).hasSize(1)
        assertThat(historyDB[0].last_read).isEqualTo(1000)
    }

    /**
     * Test to check if tracking restore works
     */
    @Test
    fun restoreTrackForManga() {
        // Create mangas
        val manga = getSingleManga("One Piece")
        val manga2 = getSingleManga("Bleach")
        manga.id = legacyBackupManager.databaseHelper.insertManga(manga).executeAsBlocking().insertedId()
        manga2.id = legacyBackupManager.databaseHelper.insertManga(manga2).executeAsBlocking().insertedId()

        // Create track and add it to database
        // This tests duplicate errors.
        val track = getSingleTrack(manga)
        track.last_chapter_read = 5
        legacyBackupManager.databaseHelper.insertTrack(track).executeAsBlocking()
        var trackDB = legacyBackupManager.databaseHelper.getTracks(manga).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(5)
        track.last_chapter_read = 7

        // Create track for different manga to test track not in database
        val track2 = getSingleTrack(manga2)
        track2.last_chapter_read = 10

        // Check parser and restore already in database
        var trackList = listOf(track)
        // Check parser
        var trackListJson = legacyBackupManager.parser.encodeToString(trackList)
        var trackListRestore = legacyBackupManager.parser.decodeFromString<List<Track>>(trackListJson)
        legacyBackupManager.restoreTrackForManga(manga, trackListRestore)

        // Assert if restore works.
        trackDB = legacyBackupManager.databaseHelper.getTracks(manga).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(7)

        // Check parser and restore already in database with lower chapter_read
        track.last_chapter_read = 5
        trackList = listOf(track)
        legacyBackupManager.restoreTrackForManga(manga, trackList)

        // Assert if restore works.
        trackDB = legacyBackupManager.databaseHelper.getTracks(manga).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(7)

        // Check parser and restore, track not in database
        trackList = listOf(track2)

        // Check parser
        trackListJson = legacyBackupManager.parser.encodeToString(trackList)
        trackListRestore = legacyBackupManager.parser.decodeFromString<List<Track>>(trackListJson)
        legacyBackupManager.restoreTrackForManga(manga2, trackListRestore)

        // Assert if restore works.
        trackDB = legacyBackupManager.databaseHelper.getTracks(manga2).executeAsBlocking()
        assertThat(trackDB).hasSize(1)
        assertThat(trackDB[0].last_chapter_read).isEqualTo(10)
    }

    private fun clearJson() {
        root = Backup()
        information = buildJsonObject {}
    }

    private fun addSingleCategory(name: String): Category {
        val category = Category.create(name)
        root.categories = listOf(category)
        return category
    }

    private fun clearDatabase() {
        db.deleteMangas().executeAsBlocking()
        db.deleteHistory().executeAsBlocking()
    }

    private fun getSingleHistory(chapter: Chapter): DHistory {
        return DHistory(chapter.url, 1000)
    }

    private fun getSingleTrack(manga: Manga): TrackImpl {
        val track = TrackImpl()
        track.title = manga.title
        track.manga_id = manga.id!!
        track.sync_id = 1
        return track
    }

    private fun getSingleManga(title: String): MangaImpl {
        val manga = MangaImpl()
        manga.source = 1
        manga.title = title
        manga.url = "/manga/$title"
        manga.favorite = true
        return manga
    }

    private fun getSingleChapter(name: String): ChapterImpl {
        val chapter = ChapterImpl()
        chapter.name = name
        chapter.url = "/read-online/$name-page-1.html"
        return chapter
    }
}
