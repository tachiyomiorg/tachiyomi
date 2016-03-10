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

public class Mangahere extends Source {

    public static final String NAME = "Mangahere";
    public static final String BASE_URL = "http://www.mangahere.co";
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/directory/%s";
    public static final String SEARCH_URL = BASE_URL + "/search.php?name=%s&page=%s&sort=views&order=za";

    public Mangahere(Context context) {
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
    public List<Manga> parsePopularMangasFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("div.directory_list > ul > li")) {
            Manga currentManga = constructPopularMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructPopularMangaFromHtmlBlock(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "div.title > a");
        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.attr("title");
        }
        return manga;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        Element next = Parser.element(parsedHtml, "div.next-page > a.next");
        return next != null ? String.format(POPULAR_MANGAS_URL, next.attr("href")) : null;
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        Elements mangaHtmlBlocks = parsedHtml.select("div.result_search > dl");
        for (Element currentHtmlBlock : mangaHtmlBlocks) {
            Manga currentManga = constructSearchMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructSearchMangaFromHtmlBlock(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "a.manga_info");
        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text();
        }
        return manga;
    }

    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        Element next = Parser.element(parsedHtml, "div.next-page > a.next");
        return next != null ? BASE_URL + next.attr("href") : null;
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
                Date withoutDay = new SimpleDateFormat("MMM d, yyyy h:mma", Locale.ENGLISH).parse(updatedDateAsString.replace("Today", ""));
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
                Date withoutDay = new SimpleDateFormat("MMM d, yyyy h:mma", Locale.ENGLISH).parse(updatedDateAsString.replace("Yesterday", ""));
                return yesterday.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return yesterday.getTimeInMillis();
            }
        } else {
            try {
                Date specificDate = new SimpleDateFormat("MMM d, yyyy h:mma", Locale.ENGLISH).parse(updatedDateAsString);

                return specificDate.getTime();
            } catch (ParseException e) {
                // Do Nothing.
            }
        }

        return 0;
    }

    public Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        int beginIndex = unparsedHtml.indexOf("<ul class=\"detail_topText\">");
        int endIndex = unparsedHtml.indexOf("</ul>", beginIndex);
        String trimmedHtml = unparsedHtml.substring(beginIndex, endIndex);

        Document parsedDocument = Jsoup.parse(trimmedHtml);
        Element detailElement = parsedDocument.select("ul.detail_topText").first();

        Manga manga = Manga.create(mangaUrl);
        manga.author = Parser.text(parsedDocument, "a[href^=http://www.mangahere.co/author/]");
        manga.artist = Parser.text(parsedDocument, "a[href^=http://www.mangahere.co/artist/]");

        String description = Parser.text(detailElement, "#show");
        if (description != null) {
            manga.description = description.substring(0, description.length() - "Show less".length());
        }
        String genres = Parser.text(detailElement, "li:eq(3)");
        if (genres != null) {
            manga.genre = genres.substring("Genre(s):".length());
        }
        manga.status = parseStatus(Parser.text(detailElement, "li:eq(6)"));

        beginIndex = unparsedHtml.indexOf("<img");
        endIndex = unparsedHtml.indexOf("/>", beginIndex);
        trimmedHtml = unparsedHtml.substring(beginIndex, endIndex + 2);

        parsedDocument = Jsoup.parse(trimmedHtml);
        manga.thumbnail_url = Parser.src(parsedDocument, "img");

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
    public List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        int beginIndex = unparsedHtml.indexOf("<ul>");
        int endIndex = unparsedHtml.indexOf("</ul>", beginIndex);
        String trimmedHtml = unparsedHtml.substring(beginIndex, endIndex);

        Document parsedDocument = Jsoup.parse(trimmedHtml);

        List<Chapter> chapterList = new ArrayList<>();

        for (Element chapterElement : parsedDocument.getElementsByTag("li")) {
            Chapter currentChapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(currentChapter);
        }
        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = chapterElement.select("a").first();
        Element dateElement = chapterElement.select("span.right").first();

        if (urlElement != null) {
            chapter.setUrl(urlElement.attr("href"));
            chapter.name = urlElement.text();
        }
        if (dateElement != null) {
            chapter.date_upload = parseDateFromElement(dateElement);
        }
        return chapter;
    }

    private long parseDateFromElement(Element dateElement) {
        String dateAsString = dateElement.text();

        if (dateAsString.contains("Today")) {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            try {
                Date withoutDay = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(dateAsString.replace("Today", ""));
                return today.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return today.getTimeInMillis();
            }
        } else if (dateAsString.contains("Yesterday")) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DATE, -1);
            yesterday.set(Calendar.HOUR_OF_DAY, 0);
            yesterday.set(Calendar.MINUTE, 0);
            yesterday.set(Calendar.SECOND, 0);
            yesterday.set(Calendar.MILLISECOND, 0);

            try {
                Date withoutDay = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(dateAsString.replace("Yesterday", ""));
                return yesterday.getTimeInMillis() + withoutDay.getTime();
            } catch (ParseException e) {
                return yesterday.getTimeInMillis();
            }
        } else {
            try {
                Date date = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH).parse(dateAsString);

                return date.getTime();
            } catch (ParseException e) {
                // Do Nothing.
            }
        }
        return 0;
    }

    @Override
    public List<String> parseHtmlToPageUrls(String unparsedHtml) {
        int beginIndex = unparsedHtml.indexOf("<div class=\"go_page clearfix\">");
        int endIndex = unparsedHtml.indexOf("</div>", beginIndex);
        String trimmedHtml = unparsedHtml.substring(beginIndex, endIndex);

        Document parsedDocument = Jsoup.parse(trimmedHtml);

        List<String> pageUrlList = new ArrayList<>();

        Elements pageUrlElements = parsedDocument.select("select.wid60").first().getElementsByTag("option");
        for (Element pageUrlElement : pageUrlElements) {
            pageUrlList.add(pageUrlElement.attr("value"));
        }

        return pageUrlList;
    }

    @Override
    public String parseHtmlToImageUrl(String unparsedHtml) {
        int beginIndex = unparsedHtml.indexOf("<section class=\"read_img\" id=\"viewer\">");
        int endIndex = unparsedHtml.indexOf("</section>", beginIndex);
        String trimmedHtml = unparsedHtml.substring(beginIndex, endIndex);

        Document parsedDocument = Jsoup.parse(trimmedHtml);

        Element imageElement = parsedDocument.getElementById("image");

        return imageElement.attr("src");
    }

}
