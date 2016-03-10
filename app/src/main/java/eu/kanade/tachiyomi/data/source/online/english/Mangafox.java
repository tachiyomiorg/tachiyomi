package eu.kanade.tachiyomi.data.source.online.english;

import android.content.Context;
import android.net.Uri;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.Language;
import eu.kanade.tachiyomi.data.source.LanguageKt;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.util.Parser;

public class Mangafox extends Source {

    public static final String NAME = "Mangafox";
    public static final String BASE_URL = "http://mangafox.me";
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/directory/%s";
    public static final String SEARCH_URL =
            BASE_URL + "/search.php?name_method=cw&advopts=1&order=za&sort=views&name=%s&page=%s";

    public Mangafox(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    public Language getLang() {
        return LanguageKt.getEN();
    }

    @Override
    protected String getInitialPopularMangasUrl() {
        return String.format(POPULAR_MANGAS_URL, "");
    }

    @Override
    protected String getInitialSearchUrl(String query) {
        return String.format(SEARCH_URL, Uri.encode(query), 1);
    }

    @Override
    protected List<Manga> parsePopularMangasFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("div#mangalist > ul.list > li")) {
            Manga currentManga = constructPopularMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructPopularMangaFromHtmlBlock(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "a.title");
        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text();
        }
        return manga;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        Element next = Parser.element(parsedHtml, "a:has(span.next)");
        return next != null ? String.format(POPULAR_MANGAS_URL, next.attr("href")) : null;
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("table#listing > tbody > tr:gt(0)")) {
            Manga currentManga = constructSearchMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructSearchMangaFromHtmlBlock(Element htmlBlock) {
        Manga mangaFromHtmlBlock = new Manga();
        mangaFromHtmlBlock.source = getId();

        Element urlElement = Parser.element(htmlBlock, "a.series_preview");
        if (urlElement != null) {
            mangaFromHtmlBlock.setUrl(urlElement.attr("href"));
            mangaFromHtmlBlock.title = urlElement.text();
        }
        return mangaFromHtmlBlock;
    }

    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        Element next = Parser.element(parsedHtml, "a:has(span.next)");
        return next != null ? BASE_URL + next.attr("href") : null;
    }

    @Override
    protected Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element infoElement = parsedDocument.select("div#title").first();
        Element rowElement = infoElement.select("table > tbody > tr:eq(1)").first();
        Element sideInfoElement = parsedDocument.select("#series_info").first();

        Manga manga = Manga.create(mangaUrl);
        manga.author = Parser.text(rowElement, "td:eq(1)");
        manga.artist = Parser.text(rowElement, "td:eq(2)");
        manga.description = Parser.text(infoElement, "p.summary");
        manga.genre = Parser.text(rowElement, "td:eq(3)");
        manga.thumbnail_url = Parser.src(sideInfoElement, "div.cover > img");
        manga.status = parseStatus(Parser.text(sideInfoElement, ".data"));

        manga.initialized = true;
        return manga;
    }

    private int parseStatus(String status) {
        if (status.contains("Ongoing")) {
            return Manga.ONGOING;
        }
        if (status.contains("Completed")) {
            return Manga.COMPLETED;
        }
        return Manga.UNKNOWN;
    }

    @Override
    protected List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<Chapter> chapterList = new ArrayList<>();

        for (Element chapterElement : parsedDocument.select("div#chapters li div")) {
            Chapter currentChapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(currentChapter);
        }
        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = chapterElement.select("a.tips").first();
        Element dateElement = chapterElement.select("span.date").first();

        if (urlElement != null) {
            chapter.setUrl(urlElement.attr("href"));
            chapter.name = urlElement.text();
        }
        if (dateElement != null) {
            chapter.date_upload = parseUpdateFromElement(dateElement);
        }
        return chapter;
    }

    private long parseUpdateFromElement(Element updateElement) {
        String updatedDateAsString = updateElement.text();

        if (updatedDateAsString.contains("Today")) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            try {
                Date withoutDay = new SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(updatedDateAsString.replace("Today", ""));
                return today.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return today.getTimeInMillis();
            }
        } else if (updatedDateAsString.contains("Yesterday")) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            yesterday.set(Calendar.HOUR_OF_DAY, 0);
            yesterday.set(Calendar.MINUTE, 0);
            yesterday.set(Calendar.SECOND, 0);
            yesterday.set(Calendar.MILLISECOND, 0);

            try {
                Date withoutDay = new SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(updatedDateAsString.replace("Yesterday", ""));
                return yesterday.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return yesterday.getTimeInMillis();
            }
        } else {
            try {
                Date specificDate = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(updatedDateAsString);

                return specificDate.getTime();
            } catch (ParseException e) {
                // Do Nothing.
            }
        }

        return 0;
    }

    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<String> pageUrlList = new ArrayList<>();

        Elements pageUrlElements = parsedDocument.select("select.m").first().select("option:not([value=0])");
        String baseUrl = parsedDocument.select("div#series a").first().attr("href").replace("1.html", "");
        for (Element pageUrlElement : pageUrlElements) {
            pageUrlList.add(baseUrl + pageUrlElement.attr("value") + ".html");
        }

        return pageUrlList;
    }

    @Override
    protected String parseHtmlToImageUrl(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element imageElement = parsedDocument.getElementById("image");
        return imageElement.attr("src");
    }
}
