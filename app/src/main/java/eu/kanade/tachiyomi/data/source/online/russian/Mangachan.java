package eu.kanade.tachiyomi.data.source.online.russian;

import android.content.Context;
import android.net.Uri;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.util.Parser;
import rx.Observable;

import java.util.regex.*;

public class Mangachan extends Source {

    public static final String NAME = "Mangachan (RU)";
    public static final String BASE_URL = "http://mangachan.ru";
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/mostfavorites/%s";
    public static final String SEARCH_URL =
            BASE_URL + "/?do=search&subaction=search&story=%s&search_start=%s";

    public Mangachan(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getId() {
        return SourceManager.MANGACHAN;
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
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

        for (Element currentHtmlBlock : parsedHtml.select("div#content > div.content_row")) {
            Manga currentManga = constructPopularMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructPopularMangaFromHtmlBlock(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "a.title_link");
        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text();
        }
        return manga;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        Element next = parsedHtml.select("a:contains(Вперед)").get(0);
        return next != null ? String.format(POPULAR_MANGAS_URL, next.attr("href")) : null;
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("div#dle-content > div.content_row")) {
            Manga currentManga = constructSearchMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructSearchMangaFromHtmlBlock(Element htmlBlock) {
        Manga mangaFromHtmlBlock = new Manga();
        mangaFromHtmlBlock.source = getId();

        Element urlElement = Parser.element(htmlBlock, "h2 > a");
        if (urlElement != null) {
            mangaFromHtmlBlock.setUrl(urlElement.attr("href"));
            mangaFromHtmlBlock.title = urlElement.text();
        }
        return mangaFromHtmlBlock;
    }

    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        Element link = parsedHtml.select("a:contains(Далее)").first();
        if (link == null) {
            return null;
        } else {
            String onclick_str = link.attr("onclick");
            String page_number = onclick_str.substring(23, onclick_str.indexOf("); return(false)"));
            return String.format(SEARCH_URL, query, page_number);
        }
    }

    @Override
    protected Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element infoElement = parsedDocument.select("table.mangatitle").first();
        String image_url = parsedDocument.select("img#cover").first().attr("src");

        Manga manga = Manga.create(mangaUrl);
        manga.author = Parser.text(infoElement, "tr:eq(2) span:eq(0)");
        manga.description = Parser.text(parsedDocument, "div#description");
        manga.genre = Parser.text(infoElement, "tr:eq(5) span:eq(0)");
        manga.thumbnail_url = BASE_URL + image_url;
        manga.status = parseStatus(Parser.text(infoElement, "tr:eq(4)"));

        manga.initialized = true;
        return manga;
    }

    private int parseStatus(String status) {
        if (status.contains("перевод продолжается")) {
            return Manga.ONGOING;
        }
        if (status.contains("перевод завершен") || status.contains("Сингл")) {
            return Manga.COMPLETED;
        }
        return Manga.UNKNOWN;
    }

    @Override
    protected List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<Chapter> chapterList = new ArrayList<>();

        for (Element chapterElement : parsedDocument.select("table.table_cha tr:gt(1)")) {
            Chapter currentChapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(currentChapter);
        }
        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = chapterElement.select("div.manga a").first();
        Element dateElement = chapterElement.select("div.date").first();

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

        try {
            Date specificDate = new SimpleDateFormat("yyyy-MM-d", Locale.ENGLISH).parse(updatedDateAsString);

            return specificDate.getTime();
        } catch (ParseException e) {
            // Do Nothing.
        }

        return 0;
    }

    // Return List of IMAGE URLS, NOT pages
    // to get page url need to parse [div style="height:155px;background-color:#ccc;"] > a
    // and on the page get the same urls but take only first one
    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {

        List<String> imageUrlList = new ArrayList<>();

        Pattern pattern = Pattern.compile("(?<=\"fullimg\":\\[).+(?=])");
        Matcher matcher = pattern.matcher(unparsedHtml);

        if (matcher.find()) {
            String str_urls = matcher.group(0);
            str_urls = str_urls.replace("\"", "");
            str_urls = str_urls.replaceAll("im.?\\.", "");

            /* for webp format
            //in web browser site returns webp format of images
            //dunno what's the difference
            str_urls = str_urls.replace("manganew", "manganew_webp");
            str_urls = str_urls.replace("jpg", "webp");
            */

            String[] urls = str_urls.split(",");
            imageUrlList = Arrays.asList(urls);
        }

        return imageUrlList;
    }

    // image_url == page_url
    @Override
    public Observable<Page> getImageUrlFromPage(final Page page) {
        page.setImageUrl(page.getUrl());
        return Observable.just(page);
    }

    // image_url == page_url
    @Override
    protected List<Page> parseFirstPage(List<Page> pages, String unparsedHtml) {
        pages.get(0).setImageUrl(pages.get(0).getUrl());
        return pages;
    }

    @Override
    protected String parseHtmlToImageUrl(String unparsedHtml) {
        return null;
    }
}
