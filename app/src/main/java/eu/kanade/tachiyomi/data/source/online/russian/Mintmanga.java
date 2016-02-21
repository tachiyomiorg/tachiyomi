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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.SourceManager;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.util.Parser;
import rx.Observable;

public class Mintmanga extends Source {

    public static final String NAME = "(RU) MintManga";
    public static final String BASE_URL = "http://mintmanga.com";
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/list?sortType=rate";
    public static final String SEARCH_URL = BASE_URL + "/search?q=%s";

    public Mintmanga(Context context) {
        super(context);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getId() {
        return SourceManager.MINTMANGA;
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

        for (Element currentHtmlBlock : parsedHtml.select("div.tiles >  div.tile")) {
            Manga currentManga = constructPopularMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructPopularMangaFromHtmlBlock(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "div.desc > h3 > a");
        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text();
        }
        return manga;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        Element next = Parser.element(parsedHtml, "span.pagination a.nextLink");
        return next != null ? String.format(POPULAR_MANGAS_URL, next.attr("href")) : null;
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("div#mangaResults div.tiles div.tile")) {
            Manga currentManga = constructSearchMangaFromHtmlBlock(currentHtmlBlock);
            mangaList.add(currentManga);
        }
        return mangaList;
    }

    private Manga constructSearchMangaFromHtmlBlock(Element htmlBlock) {
        Manga mangaFromHtmlBlock = new Manga();
        mangaFromHtmlBlock.source = getId();

        Element urlElement = Parser.element(htmlBlock, "div.desc > h3 > a");
        if (urlElement != null) {
            mangaFromHtmlBlock.setUrl(urlElement.attr("href"));
            mangaFromHtmlBlock.title = urlElement.text();
        }
        return mangaFromHtmlBlock;
    }

    // loads 200 results. so no need in next search url
    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        return null;
    }

    @Override
    protected Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element mainElement = parsedDocument.select("div.leftContent").first();
        Element infoElement = mainElement.select("div.subject-meta").first();

        Manga manga = Manga.create(mangaUrl);
        if (Parser.text(infoElement, "p:eq(1)").equals("Сингл")) {
            manga.status = Manga.COMPLETED;
        } else {
            manga.status = parseStatus(Parser.text(infoElement, "p:eq(3)"));
        }
        manga.author = Parser.text(infoElement, "span.elem_author ");
        manga.genre = Parser.allText(infoElement, "span.elem_genre ").replaceAll(" ,", ",");
        manga.thumbnail_url = Parser.src(mainElement, "div.picture-fotorama > img:eq(0)");
        manga.description = Parser.text(mainElement, "div.manga-description");
        manga.initialized = true;
        return manga;
    }

    private int parseStatus(String status) {
        if (status.contains("продолжается")) {
            return Manga.ONGOING;
        }
        if (status.contains("завершен")) {
            return Manga.COMPLETED;
        }
        return Manga.UNKNOWN;
    }

    @Override
    protected List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<Chapter> chapterList = new ArrayList<>();

        for (Element chapterElement : parsedDocument.select("div.chapters-link tbody tr")) {
            Chapter currentChapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(currentChapter);
        }
        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = chapterElement.select("td > a").first();
        Element dateElement = chapterElement.select("td").last();

        if (urlElement != null) {
            chapter.setUrl(urlElement.attr("href") + "?mature=1");
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
            Date specificDate = new SimpleDateFormat("d/MM/yyyy", Locale.ENGLISH).parse(updatedDateAsString);

            return specificDate.getTime();
        } catch (ParseException e) {
            // Do Nothing.
        }

        return 0;
    }

    // Return List of IMAGE URLS, NOT pages
    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {

        List<String> imageUrlList = new ArrayList<>();

        Pattern pattern = Pattern.compile("(?<=rm_h\\.init\\( \\[\\[).+(?=]],)");
        Matcher matcher = pattern.matcher(unparsedHtml);

        if (matcher.find()) {
            String str_urls = matcher.group(0);
            str_urls = str_urls.replace("\'", "");
            str_urls = str_urls.replace("\"", "");
            String[] arr_of_url_parts = str_urls.split("],\\[");

            for (String url_parts : arr_of_url_parts) {
                String[] arr_of_parts = url_parts.split(",");
                imageUrlList.add(arr_of_parts[1] + arr_of_parts[0] + arr_of_parts[2]);
            }

            /*
            // example:
            // ['auto/12/56','http://e7.postfact.ru/',"/51/01.jpg_res.jpg",850,1230]
            // url = 2, 1, 3
            // url = http://e7.postfact.ru/auto/12/56/51/01.jpg_res.jpg
            */
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
