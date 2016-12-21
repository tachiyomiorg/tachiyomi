package eu.kanade.tachiyomi.data.backup

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import uy.kohesive.injekt.injectLazy
import java.util.*

@Config(constants = BuildConfig::class, sdk = intArrayOf(Build.VERSION_CODES.LOLLIPOP))
@RunWith(CustomRobolectricGradleTestRunner::class)
class BackupTest {

    val gson: Gson by injectLazy()

    lateinit var db: DatabaseHelper

    lateinit var backupManager: BackupManager

    lateinit var root: JsonObject

    @Before
    fun setup() {
        val app = RuntimeEnvironment.application
        db = DatabaseHelper(app)
        backupManager = BackupManager(db)
        root = JsonObject()
    }

    @Test
    fun testRestoreCategory() {
        val catName = "cat"
        root = createRootJson(null, toJson(createCategories(catName)))
        backupManager.restoreFromJson(root)

        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(1)
        assertThat(dbCats[0].name).isEqualTo(catName)
    }

    @Test
    fun testRestoreEmptyCategory() {
        root = createRootJson(null, toJson(ArrayList<Any>()))
        backupManager.restoreFromJson(root)
        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).isEmpty()
    }

    @Test
    fun testRestoreExistingCategory() {
        val catName = "cat"
        db.insertCategory(createCategory(catName)).executeAsBlocking()

        root = createRootJson(null, toJson(createCategories(catName)))
        backupManager.restoreFromJson(root)

        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(1)
        assertThat(dbCats[0].name).isEqualTo(catName)
    }

    @Test
    fun testRestoreCategories() {
        root = createRootJson(null, toJson(createCategories("cat", "cat2", "cat3")))
        backupManager.restoreFromJson(root)

        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(3)
    }

    @Test
    fun testRestoreExistingCategories() {
        db.insertCategories(createCategories("cat", "cat2")).executeAsBlocking()

        root = createRootJson(null, toJson(createCategories("cat", "cat2", "cat3")))
        backupManager.restoreFromJson(root)

        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(3)
    }

    @Test
    fun testRestoreExistingCategoriesAlt() {
        db.insertCategories(createCategories("cat", "cat2", "cat3")).executeAsBlocking()

        root = createRootJson(null, toJson(createCategories("cat", "cat2")))
        backupManager.restoreFromJson(root)

        val dbCats = db.getCategories().executeAsBlocking()
        assertThat(dbCats).hasSize(3)
    }

    @Test
    fun testRestoreManga() {
        val mangaName = "title"
        val mangas = createMangas(mangaName)
        val elements = ArrayList<JsonElement>()
        for (manga in mangas) {
            val entry = JsonObject()
            entry.add("manga", toJson(manga))
            elements.add(entry)
        }
        root = createRootJson(toJson(elements), null)
        backupManager.restoreFromJson(root)

        val dbMangas = db.getMangas().executeAsBlocking()
        assertThat(dbMangas).hasSize(1)
        assertThat(dbMangas[0].title).isEqualTo(mangaName)
    }

    @Test
    fun testRestoreExistingManga() {
        val mangaName = "title"
        val manga = createManga(mangaName)

        db.insertManga(manga).executeAsBlocking()

        val elements = ArrayList<JsonElement>()
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        elements.add(entry)

        root = createRootJson(toJson(elements), null)
        backupManager.restoreFromJson(root)

        val dbMangas = db.getMangas().executeAsBlocking()
        assertThat(dbMangas).hasSize(1)
    }

    @Test
    fun testRestoreExistingMangaWithUpdatedFields() {
        // Store a manga in db
        val mangaName = "title"
        val updatedThumbnailUrl = "updated thumbnail url"
        var manga = createManga(mangaName)
        manga.chapter_flags = 1024
        manga.thumbnail_url = updatedThumbnailUrl
        db.insertManga(manga).executeAsBlocking()

        // Add an entry for a new manga with different attributes
        manga = createManga(mangaName)
        manga.chapter_flags = 512
        val entry = JsonObject()
        entry.add("manga", toJson(manga))

        // Append the entry to the backup list
        val elements = ArrayList<JsonElement>()
        elements.add(entry)

        // Restore from json
        root = createRootJson(toJson(elements), null)
        backupManager.restoreFromJson(root)

        val dbMangas = db.getMangas().executeAsBlocking()
        assertThat(dbMangas).hasSize(1)
        assertThat(dbMangas[0].thumbnail_url).isEqualTo(updatedThumbnailUrl)
        assertThat(dbMangas[0].chapter_flags).isEqualTo(512)
    }

    @Test
    fun testRestoreChaptersForManga() {
        // Create a manga and 3 chapters
        val manga = createManga("title")
        manga.id = 1L
        val chapters = createChapters(manga, "1", "2", "3")

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("chapters", toJson(chapters))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), null)
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(1).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val dbChapters = db.getChapters(dbManga!!).executeAsBlocking()
        assertThat(dbChapters).hasSize(3)
    }

    @Test
    fun testRestoreChaptersForExistingManga() {
        val mangaId: Long = 3
        // Create a manga and 3 chapters
        val manga = createManga("title")
        manga.id = mangaId
        val chapters = createChapters(manga, "1", "2", "3")
        db.insertManga(manga).executeAsBlocking()

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("chapters", toJson(chapters))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), null)
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(mangaId).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val dbChapters = db.getChapters(dbManga!!).executeAsBlocking()
        assertThat(dbChapters).hasSize(3)
    }

    @Test
    fun testRestoreExistingChaptersForExistingManga() {
        val mangaId: Long = 5
        // Store a manga and 3 chapters
        val manga = createManga("title")
        manga.id = mangaId
        var chapters = createChapters(manga, "1", "2", "3")
        db.insertManga(manga).executeAsBlocking()
        db.insertChapters(chapters).executeAsBlocking()

        // The backup contains a existing chapter and a new one, so it should have 4 chapters
        chapters = createChapters(manga, "3", "4")

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("chapters", toJson(chapters))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), null)
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(mangaId).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val dbChapters = db.getChapters(dbManga!!).executeAsBlocking()
        assertThat(dbChapters).hasSize(4)
    }

    @Test
    fun testRestoreCategoriesForManga() {
        // Create a manga
        val manga = createManga("title")

        // Create categories
        val categories = createCategories("cat1", "cat2", "cat3")

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("categories", toJson(createStringCategories("cat1")))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), toJson(categories))
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(1).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val result = db.getCategoriesForManga(dbManga!!).executeAsBlocking()

        assertThat(result).hasSize(1)
        assertThat(result).contains(Category.create("cat1"))
        assertThat(result).doesNotContain(Category.create("cat2"))
    }

    @Test
    fun testRestoreCategoriesForExistingManga() {
        // Store a manga
        val manga = createManga("title")
        db.insertManga(manga).executeAsBlocking()

        // Create categories
        val categories = createCategories("cat1", "cat2", "cat3")

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("categories", toJson(createStringCategories("cat1")))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), toJson(categories))
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(1).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val result = db.getCategoriesForManga(dbManga!!).executeAsBlocking()

        assertThat(result).hasSize(1)
        assertThat(result).contains(Category.create("cat1"))
        assertThat(result).doesNotContain(Category.create("cat2"))
    }

    @Test
    fun testRestoreMultipleCategoriesForManga() {
        // Create a manga
        val manga = createManga("title")

        // Create categories
        val categories = createCategories("cat1", "cat2", "cat3")

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("categories", toJson(createStringCategories("cat1", "cat3")))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), toJson(categories))
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(1).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val result = db.getCategoriesForManga(dbManga!!).executeAsBlocking()

        assertThat(result).hasSize(2)
        assertThat(result).contains(Category.create("cat1"), Category.create("cat3"))
        assertThat(result).doesNotContain(Category.create("cat2"))
    }

    @Test
    fun testRestoreMultipleCategoriesForExistingMangaAndCategory() {
        // Store a manga and a category
        val manga = createManga("title")
        manga.id = 1L
        db.insertManga(manga).executeAsBlocking()

        val cat = createCategory("cat1")
        cat.id = 1
        db.insertCategory(cat).executeAsBlocking()
        db.insertMangaCategory(MangaCategory.create(manga, cat)).executeAsBlocking()

        // Create categories
        val categories = createCategories("cat1", "cat2", "cat3")

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("categories", toJson(createStringCategories("cat1", "cat2")))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), toJson(categories))
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(1).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val result = db.getCategoriesForManga(dbManga!!).executeAsBlocking()

        assertThat(result).hasSize(2)
        assertThat(result).contains(Category.create("cat1"), Category.create("cat2"))
        assertThat(result).doesNotContain(Category.create("cat3"))
    }

    @Test
    fun testRestoreSyncForManga() {
        // Create a manga and track
        val manga = createManga("title")
        manga.id = 1L

        val track = createTrack(manga, 1, 2, 3)

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("sync", toJson(track))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), null)
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(1).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val dbSync = db.getTracks(dbManga!!).executeAsBlocking()
        assertThat(dbSync).hasSize(3)
    }

    @Test
    fun testRestoreSyncForExistingManga() {
        val mangaId: Long = 3
        // Create a manga and 3 sync
        val manga = createManga("title")
        manga.id = mangaId
        val track = createTrack(manga, 1, 2, 3)
        db.insertManga(manga).executeAsBlocking()

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("sync", toJson(track))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), null)
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(mangaId).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val dbSync = db.getTracks(dbManga!!).executeAsBlocking()
        assertThat(dbSync).hasSize(3)
    }

    @Test
    fun testRestoreExistingSyncForExistingManga() {
        val mangaId: Long = 5
        // Store a manga and 3 sync
        val manga = createManga("title")
        manga.id = mangaId
        var track = createTrack(manga, 1, 2, 3)
        db.insertManga(manga).executeAsBlocking()
        db.insertTracks(track).executeAsBlocking()

        // The backup contains a existing sync and a new one, so it should have 4 sync
        track = createTrack(manga, 3, 4)

        // Add an entry for the manga
        val entry = JsonObject()
        entry.add("manga", toJson(manga))
        entry.add("sync", toJson(track))

        // Append the entry to the backup list
        val mangas = ArrayList<JsonElement>()
        mangas.add(entry)

        // Restore from json
        root = createRootJson(toJson(mangas), null)
        backupManager.restoreFromJson(root)

        val dbManga = db.getManga(mangaId).executeAsBlocking()
        assertThat(dbManga).isNotNull()

        val dbSync = db.getTracks(dbManga!!).executeAsBlocking()
        assertThat(dbSync).hasSize(4)
    }

    private fun createRootJson(mangas: JsonElement?, categories: JsonElement?): JsonObject {
        val root = JsonObject()
        if (mangas != null)
            root.add("mangas", mangas)
        if (categories != null)
            root.add("categories", categories)
        return root
    }

    private fun createCategory(name: String): Category {
        val c = CategoryImpl()
        c.name = name
        return c
    }

    private fun createCategories(vararg names: String): List<Category> {
        val cats = ArrayList<Category>()
        for (name in names) {
            cats.add(createCategory(name))
        }
        return cats
    }

    private fun createStringCategories(vararg names: String): List<String> {
        val cats = ArrayList<String>()
        for (name in names) {
            cats.add(name)
        }
        return cats
    }

    private fun createManga(title: String): Manga {
        val m = Manga.create(1)
        m.title = title
        m.author = ""
        m.artist = ""
        m.thumbnail_url = ""
        m.genre = "a list of genres"
        m.description = "long description"
        m.url = "url to manga"
        m.favorite = true
        return m
    }

    private fun createMangas(vararg titles: String): List<Manga> {
        val mangas = ArrayList<Manga>()
        for (title in titles) {
            mangas.add(createManga(title))
        }
        return mangas
    }

    private fun createChapter(manga: Manga, url: String): Chapter {
        val c = Chapter.create()
        c.url = url
        c.name = url
        c.manga_id = manga.id
        return c
    }

    private fun createChapters(manga: Manga, vararg urls: String): List<Chapter> {
        val chapters = ArrayList<Chapter>()
        for (url in urls) {
            chapters.add(createChapter(manga, url))
        }
        return chapters
    }

    private fun createTrack(manga: Manga, syncId: Int): Track {
        val m = Track.create(syncId)
        m.manga_id = manga.id!!
        m.title = "title"
        return m
    }

    private fun createTrack(manga: Manga, vararg syncIds: Int): List<Track> {
        val ms = ArrayList<Track>()
        for (title in syncIds) {
            ms.add(createTrack(manga, title))
        }
        return ms
    }

    private fun toJson(element: Any): JsonElement {
        return gson.toJsonTree(element)
    }

}