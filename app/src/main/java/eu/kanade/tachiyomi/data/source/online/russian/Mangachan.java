package eu.kanade.tachiyomi.data.source.online.russian;

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
import eu.kanade.tachiyomi.data.source.Language;
import eu.kanade.tachiyomi.data.source.LanguageKt;
import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.util.Parser;

public class Mangachan extends Source {

    public static final String NAME = "Mangachan";
    public static final String BASE_URL = "http://mangachan.ru";
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/mostfavorites";
    public static final String SEARCH_URL = BASE_URL + "/?do=search&subaction=search&story=%s";

    public Mangachan(Context context) {
        super(context);
    }

    @Override
    public Language getLang() {
        return LanguageKt.getRU();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    @Override
    protected String getInitialPopularMangasUrl() {
        return POPULAR_MANGAS_URL;
    }

    @Override
    protected String getInitialSearchUrl(String query) {
        return String.format(SEARCH_URL, Uri.encode(query));
    }

    @Override
    protected List<Manga> parsePopularMangasFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        for (Element currentHtmlBlock : parsedHtml.select("div.content_row")) {
            Manga manga = constructPopularMangaFromHtml(currentHtmlBlock);
            mangaList.add(manga);
        }

        return mangaList;
    }

    private Manga constructPopularMangaFromHtml(Element currentHtmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = currentHtmlBlock.getElementsByTag("h2").select("a").first();
        Element imgElement = currentHtmlBlock.getElementsByClass("manga_images").select("img").first();

        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text();
        }

        if (imgElement != null) {
            manga.thumbnail_url = BASE_URL + imgElement.attr("src");
        }

        return manga;
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        String path = Parser.href(parsedHtml, "a:contains(Вперед)");
        return path != null ? POPULAR_MANGAS_URL + path : null;
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

        Element infoElement = parsedDocument.getElementsByClass("mangatitle").first();
        String description = parsedDocument.getElementById("description").text();

        Manga manga = Manga.create(mangaUrl);

        manga.author = infoElement.select("tr:eq(2) td:eq(1)").text();
        manga.genre = infoElement.select("tr:eq(5) td:eq(1)").text();
        manga.status = parseStatus(infoElement.select("tr:eq(4) td:eq(1)").text());

        manga.description = description.replaceAll("Прислать описание", "");

        manga.initialized = true;
        return manga;
    }

    private int parseStatus(String status) {
        if (status.contains("перевод продолжается")) {
            return Manga.ONGOING;
        } else if (status.contains("перевод завершен")) {
            return Manga.COMPLETED;
        } else return Manga.UNKNOWN;
    }

    @Override
    protected List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);
        List<Chapter> chapterList = new ArrayList<>();

        for (Element chapterElement : parsedDocument.select("table.table_cha tr:gt(1)")) {
            Chapter chapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(chapter);
        }
        return chapterList;
    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = chapterElement.select("a").first();
        String date = Parser.text(chapterElement, "div.date");

        if (urlElement != null) {
            chapter.name = urlElement.text();
            chapter.url = urlElement.attr("href");
        }

        if (date != null) {
            try {
                chapter.date_upload = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(date).getTime();
            } catch (ParseException e) { /* Ignore */ }
        }
        return chapter;
    }

    // Without this extra chapters are in the wrong place in the list
    @Override
    public void parseChapterNumber(Chapter chapter) {
        // For chapters with url like /online/254903-fairy-tail_v56_ch474.html
        String url = chapter.url.replace(".html", "");
        Pattern pattern = Pattern.compile("\\d+_ch[\\d.]+");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String[] parts = matcher.group().split("_ch");
            chapter.chapter_number = Float.parseFloat(parts[0] + "." + AddZero(parts[1]));
        } else { // For chapters with url like /online/61216-3298.html
            String name = chapter.name;
            name = name.replaceAll("[\\s\\d\\w\\W]+v", "");
            String volume = name.substring(0, name.indexOf(" - "));
            String[] parts = name.replaceFirst("\\d+ - ", "").split(" ");

            chapter.chapter_number = Float.parseFloat(volume + "." + AddZero(parts[0]));
        }
    }

    private String AddZero(String num) {
        if (Float.parseFloat(num) < 1000f) {
            num = "0" + num.replace(".", "");
        }
        if (Float.parseFloat(num) < 100f) {
            num = "0" + num.replace(".", "");
        }
        if (Float.parseFloat(num) < 10f) {
            num = "0" + num.replace(".", "");
        }
        return num;
    }

    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {
        ArrayList<String> pages = new ArrayList<>();

        int beginIndex = unparsedHtml.indexOf("fullimg\":[");
        int endIndex = unparsedHtml.indexOf(']', beginIndex);

        String trimmedHtml = unparsedHtml.substring(beginIndex + 10, endIndex);
        trimmedHtml = trimmedHtml.replaceAll("\"", "");

        String[] pageUrls = trimmedHtml.split(",");
        for (int i = 0; i < pageUrls.length; i++) {
            pages.add("");
        }

        return pages;
    }

    @Override
    protected List<Page> parseFirstPage(List<? extends Page> pages, String unparsedHtml) {
        int beginIndex = unparsedHtml.indexOf("fullimg\":[");
        int endIndex = unparsedHtml.indexOf(']', beginIndex);

        String trimmedHtml = unparsedHtml.substring(beginIndex + 10, endIndex);
        trimmedHtml = trimmedHtml.replaceAll("\"", "");

        String[] pageUrls = trimmedHtml.split(",");
        for (int i = 0; i < pageUrls.length; i++) {
            pages.get(i).setImageUrl(pageUrls[i].replaceAll("im.?\\.", ""));
        }

        return (List<Page>) pages;
    }

    @Override
    protected String parseHtmlToImageUrl(String unparsedHtml) {
        return null;
    }
}
