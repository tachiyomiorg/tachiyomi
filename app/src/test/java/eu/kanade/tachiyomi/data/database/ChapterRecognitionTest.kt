package eu.kanade.tachiyomi.data.database

import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.util.chapter.ChapterRecognition
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ChapterRecognitionTest {
    /**
     * The manga containing manga title
     */
    lateinit var manga: Manga

    /**
     * The chapter containing chapter name
     */
    lateinit var chapter: Chapter

    /**
     * Set chapter title
     * @param name name of chapter
     * @return chapter object
     */
    private fun createChapter(name: String): Chapter {
        chapter = Chapter.create()
        chapter.name = name
        return chapter
    }

    /**
     * Set manga title
     * @param title title of manga
     * @return manga object
     */
    private fun createManga(title: String): Manga {
        manga.title = title
        return manga
    }

    /**
     * Called before test
     */
    @Before
    fun setup() {
        manga = Manga.create(0).apply { title = "random" }
        chapter = Chapter.create()
    }

    /**
     * Ch.xx base case
     */
    @Test
    fun ChCaseBase() {
        createManga("Mokushiroku Alice")

        createChapter("Mokushiroku Alice Vol.1 Ch.4: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4f)
    }

    /**
     * Ch. xx base case but space after period
     */
    @Test
    fun ChCaseBase2() {
        createManga("Mokushiroku Alice")

        createChapter("Mokushiroku Alice Vol. 1 Ch. 4: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4f)
    }

    /**
     * Ch.xx.x base case
     */
    @Test
    fun ChCaseDecimal() {
        createManga("Mokushiroku Alice")

        createChapter("Mokushiroku Alice Vol.1 Ch.4.1: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4.1f)

        createChapter("Mokushiroku Alice Vol.1 Ch.4.4: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4.4f)
    }

    /**
     * Ch.xx.a base case
     */
    @Test
    fun ChCaseAlpha() {
        createManga("Mokushiroku Alice")

        createChapter("Mokushiroku Alice Vol.1 Ch.4.a: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4.1f)

        createChapter("Mokushiroku Alice Vol.1 Ch.4.b: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4.2f)

        createChapter("Mokushiroku Alice Vol.1 Ch.4.extra: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4.99f)
    }

    /**
     * Name containing one number base case
     */
    @Test
    fun OneNumberCaseBase() {
        createManga("Bleach")

        createChapter("Bleach 567 Down With Snowwhite")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(567f)
    }

    /**
     * Name containing one number and decimal case
     */
    @Test
    fun OneNumberCaseDecimal() {
        createManga("Bleach")

        createChapter("Bleach 567.1 Down With Snowwhite")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(567.1f)

        createChapter("Bleach 567.4 Down With Snowwhite")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(567.4f)
    }

    /**
     * Name containing one number and alpha case
     */
    @Test
    fun OneNumberCaseAlpha() {
        createManga("Bleach")

        createChapter("Bleach 567.a Down With Snowwhite")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(567.1f)

        createChapter("Bleach 567.b Down With Snowwhite")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(567.2f)

        createChapter("Bleach 567.extra Down With Snowwhite")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(567.99f)
    }

    /**
     * Chapter containing manga title and number base case
     */
    @Test
    fun MangaTitleCaseBase() {
        createManga("Solanin")

        createChapter("Solanin 028 Vol. 2")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28f)
    }

    /**
     * Chapter containing manga title and number decimal case
     */
    @Test
    fun MangaTitleCaseDecimal() {
        createManga("Solanin")

        createChapter("Solanin 028.1 Vol. 2")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.1f)

        createChapter("Solanin 028.4 Vol. 2")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.4f)
    }

    /**
     * Chapter containing manga title and number alpha case
     */
    @Test
    fun MangaTitleCaseAlpha() {
        createManga("Solanin")

        createChapter("Solanin 028.a Vol. 2")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.1f)

        createChapter("Solanin 028.b Vol. 2")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.2f)

        createChapter("Solanin 028.extra Vol. 2")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.99f)
    }

    /**
     * Extreme base case
     */
    @Test
    fun ExtremeCaseBase() {
        createManga("Onepunch-Man")

        createChapter("Onepunch-Man Punch Ver002 028")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28f)
    }

    /**
     * Extreme base case decimal
     */
    @Test
    fun ExtremeCaseDecimal() {
        createManga("Onepunch-Man")

        createChapter("Onepunch-Man Punch Ver002 028.1")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.1f)

        createChapter("Onepunch-Man Punch Ver002 028.4")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.4f)
    }

    /**
     * Extreme base case alpha
     */
    @Test
    fun ExtremeCaseAlpha() {
        createManga("Onepunch-Man")

        createChapter("Onepunch-Man Punch Ver002 028.a")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.1f)

        createChapter("Onepunch-Man Punch Ver002 028.b")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.2f)

        createChapter("Onepunch-Man Punch Ver002 028.extra")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(28.99f)
    }

    /**
     * Chapter containing .v2
     */
    @Test
    fun dotV2Case() {
        createChapter("Vol.1 Ch.5v.2: Alones")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(5f)
    }

    /**
     * Check for case with number in manga title
     */
    @Test
    fun numberInMangaTitleCase() {
        createManga("Ayame 14")
        createChapter("Ayame 14 1 - The summer of 14")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(1f)
    }

    /**
     * Case with space between ch. x
     */
    @Test
    fun spaceAfterChapterCase() {
        createManga("Mokushiroku Alice")
        createChapter("Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(4f)
    }

    /**
     * Chapter containing mar(ch)
     */
    @Test
    fun marchInChapterCase() {
        createManga("Ayame 14")
        createChapter("Vol.1 Ch.1: March 25 (First Day Cohabiting)")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(1f)
    }

    /**
     * Chapter containing range
     */
    @Test
    fun rangeInChapterCase() {
        createChapter("Ch.191-200 Read Online")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(191f)
    }

    /**
     * Chapter containing multiple zeros
     */
    @Test
    fun multipleZerosCase() {
        createChapter("Vol.001 Ch.003: Kaguya Doesn't Know Much")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(3f)
    }

    /**
     * Chapter with version before number
     */
    @Test
    fun chapterBeforeNumberCase() {
        createManga("Onepunch-Man")
        createChapter("Onepunch-Man Punch Ver002 086 : Creeping Darkness [3]")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(86f)
    }

    /**
     * Case with version attached to chapter number
     */
    @Test
    fun vAttachedToChapterCase() {
        createManga("Ansatsu Kyoushitsu")
        createChapter("Ansatsu Kyoushitsu 011v002: Assembly Time")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(11f)
    }

    /**
     * Case where the chapter title contains the chapter
     * But wait it's not actual the chapter number.
     */
    @Test
    fun NumberAfterMangaTitleWithChapterInChapterTitleCase() {
        createChapter("Tokyo ESP 027: Part 002: Chapter 001")
        createManga("Tokyo ESP")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(027f)
    }

    /**
     * unParsable chapter
     */
    @Test
    fun unParsableCase() {
        createChapter("Foo")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(-1f)
    }

    /**
     * chapter with time in title
     */
    @Test
    fun timeChapterCase() {
        createChapter("Fairy Tail 404: 00:00")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404f)
    }

    /**
     * chapter with alpha without dot
     */
    @Test
    fun alphaWithoutDotCase() {
        createChapter("Asu No Yoichi 19a")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(19.1f)
    }

    /**
     * Chapter title containing extra and vol
     */
    @Test
    fun chapterContainingExtraCase() {
        createManga("Fairy Tail")

        createChapter("Fairy Tail 404.extravol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.99f)

        createChapter("Fairy Tail 404 extravol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.99f)

        createChapter("Fairy Tail 404.evol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.5f)
    }

    /**
     * Chapter title containing omake (japanese extra) and vol
     */
    @Test
    fun chapterContainingOmakeCase() {
        createManga("Fairy Tail")

        createChapter("Fairy Tail 404.omakevol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.98f)

        createChapter("Fairy Tail 404 omakevol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.98f)

        createChapter("Fairy Tail 404.ovol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.15f)
    }

    /**
     * Chapter title containing special and vol
     */
    @Test
    fun chapterContainingSpecialCase() {
        createManga("Fairy Tail")

        createChapter("Fairy Tail 404.specialvol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.97f)

        createChapter("Fairy Tail 404 specialvol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.97f)

        createChapter("Fairy Tail 404.svol002")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(404.19f)
    }

    /**
     * Chapter title containing comma's
     */
    @Test
    fun chapterContainingCommasCase() {
        createManga("One Piece")

        createChapter("One Piece 300,a")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(300.1f)

        createChapter("One Piece Ch,123,extra")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(123.99f)

        createChapter("One Piece the sunny, goes swimming 024,005")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(24.005f)
    }

    /**
     * Test for chapters containing season
     */
    @Test
    fun chapterContainingSeasonCase() {
        createManga("D.I.C.E")

        createChapter("D.I.C.E[Season 001] Ep. 007")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(7f)
    }

    /**
     * Test for chapters in format sx - chapter xx
     */
    @Test
    fun chapterContainingSeasonCase2() {
        createManga("The Gamer")

        createChapter("S3 - Chapter 20")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(20f)
    }

    /**
     * Test for chapters ending with s
     */
    @Test
    fun chaptersEndingWithS() {
        createManga("One Outs")

        createChapter("One Outs 001")
        ChapterRecognition.parseChapterNumber(chapter, manga)
        assertThat(chapter.chapter_number).isEqualTo(1f)
    }
}
