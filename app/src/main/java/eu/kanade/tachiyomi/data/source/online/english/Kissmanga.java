package eu.kanade.tachiyomi.data.source.online.english;

import android.content.Context;
import android.net.Uri;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.network.ReqKt;
import eu.kanade.tachiyomi.data.source.Language;
import eu.kanade.tachiyomi.data.source.LanguageKt;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.util.Parser;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;

public class Kissmanga extends Source {

    public static final String NAME = "Kissmanga";
    public static final String HOST = "kissmanga.com";
    public static final String IP = "93.174.95.110";
    public static final String BASE_URL = "http://" + IP;
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/MangaList/MostPopular?page=%s";
    public static final String SEARCH_URL = BASE_URL + "/AdvanceSearch";

    public Kissmanga(Context context) {
        super(context);
    }

    @Override
    protected Headers.Builder headersBuilder() {
        Headers.Builder builder = super.headersBuilder();
        builder.add("Host", HOST);
        return builder;
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
        return String.format(POPULAR_MANGAS_URL, 1);
    }

    @Override
    protected String getInitialSearchUrl(String query) {
        return SEARCH_URL;
    }

    @Override
    protected Request searchMangaRequest(MangasPage page, String query) {
        if (page.page == 1) {
            page.url = getInitialSearchUrl(query);
        }

        FormBody.Builder form = new FormBody.Builder();
        form.add("authorArtist", "");
        form.add("mangaName", query);
        form.add("status", "");
        form.add("genres", "");

        return ReqKt.post(page.url, requestHeaders, form.build());
    }

    @Override
    protected Request pageListRequest(String chapterUrl) {
        return ReqKt.post(getBaseUrl() + chapterUrl, requestHeaders);
    }

    @Override
    protected Request imageRequest(Page page) {
        return ReqKt.get(page.getImageUrl());
    }

    @Override
    protected List<Manga> parsePopularMangasFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("table.listing tr:gt(1)")) {
            Manga manga = constructPopularMangaFromHtml(currentHtmlBlock);
            mangaList.add(manga);
        }

        return mangaList;
    }

    private Manga constructPopularMangaFromHtml(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "td a:eq(0)");

        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text();
        }

        return manga;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        String path = Parser.href(parsedHtml, "li > a:contains(› Next)");
        return path != null ? BASE_URL + path : null;
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        return parsePopularMangasFromHtml(parsedHtml);
    }

    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        return null;
    }

    @Override
    protected Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);
        Element infoElement = parsedDocument.select("div.barContent").first();

        Manga manga = Manga.create(mangaUrl);
        manga.title = Parser.text(infoElement, "a.bigChar");
        manga.author = Parser.text(infoElement, "p:has(span:contains(Author:)) > a");
        manga.genre = Parser.allText(infoElement, "p:has(span:contains(Genres:)) > *:gt(0)");
        manga.description = Parser.allText(infoElement, "p:has(span:contains(Summary:)) ~ p");
        manga.status = parseStatus(Parser.text(infoElement, "p:has(span:contains(Status:))"));

        String thumbnail = Parser.src(parsedDocument, ".rightBox:eq(0) img");
        if (thumbnail != null) {
            manga.thumbnail_url = Uri.parse(thumbnail).buildUpon().authority(IP).toString();
        }

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

        for (Element chapterElement : parsedDocument.select("table.listing tr:gt(1)")) {
            Chapter chapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(chapter);
        }

        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = Parser.element(chapterElement, "a");
        String date = Parser.text(chapterElement, "td:eq(1)");

        if (urlElement != null) {
            chapter.setUrl(urlElement.attr("href"));
            chapter.name = urlElement.text();
        }
        if (date != null) {
            try {
                chapter.date_upload = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse(date).getTime();
            } catch (ParseException e) { /* Ignore */ }
        }
        return chapter;
    }

    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);
        List<String> pageUrlList = new ArrayList<>();

        int numImages = parsedDocument.select("#divImage img").size();

        for (int i = 0; i < numImages; i++) {
            pageUrlList.add("");
        }
        return pageUrlList;
    }

    @Override
    protected List<Page> parseFirstPage(List<Page> pages, String unparsedHtml) {
        Pattern p = Pattern.compile("lstImages.push\\(\"(.+?)\"");
        Matcher m = p.matcher(unparsedHtml);

        int i = 0;
        while (m.find()) {
            pages.get(i++).setImageUrl(m.group(1));
        }
        return pages;
    }

    @Override
    protected String parseHtmlToImageUrl(String unparsedHtml) {
        return null;
    }

}
