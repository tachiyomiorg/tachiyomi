package eu.kanade.tachiyomi.data.source.online.english;

import android.content.Context;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.network.ReqKt;
import eu.kanade.tachiyomi.data.source.Language;
import eu.kanade.tachiyomi.data.source.LanguageKt;
import eu.kanade.tachiyomi.data.source.base.LoginSource;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import eu.kanade.tachiyomi.data.source.model.Page;
import eu.kanade.tachiyomi.util.Parser;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;

public class Batoto extends LoginSource {

    public static final String NAME = "Batoto";
    public static final String BASE_URL = "http://bato.to";
    public static final String POPULAR_MANGAS_URL = BASE_URL + "/search_ajax?order_cond=views&order=desc&p=%s";
    public static final String SEARCH_URL = BASE_URL + "/search_ajax?name=%s&p=%s";
    public static final String CHAPTER_URL = BASE_URL + "/areader?id=%s&p=1";
    public static final String PAGE_URL = BASE_URL + "/areader?id=%s&p=%s";
    public static final String MANGA_URL = BASE_URL + "/comic_pop?id=%s";
    public static final String LOGIN_URL = BASE_URL + "/forums/index.php?app=core&module=global&section=login";

    public static final Pattern staffNotice = Pattern.compile("=+Batoto Staff Notice=+([^=]+)==+", Pattern.CASE_INSENSITIVE);

    private Pattern datePattern;
    private Map<String, Integer> dateFields;

    public Batoto(Context context) {
        super(context);

        datePattern = Pattern.compile("(\\d+|A|An)\\s+(.*?)s? ago.*");
        dateFields = new HashMap<String, Integer>() {{
            put("second", Calendar.SECOND);
            put("minute", Calendar.MINUTE);
            put("hour",   Calendar.HOUR);
            put("day",    Calendar.DATE);
            put("week",   Calendar.WEEK_OF_YEAR);
            put("month",  Calendar.MONTH);
            put("year",   Calendar.YEAR);
        }};
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
    protected Headers.Builder headersBuilder() {
        Headers.Builder builder = super.headersBuilder();
        builder.add("Cookie", "lang_option=English");
        builder.add("Referer", "http://bato.to/reader");
        return builder;
    }

    @Override
    public String getInitialPopularMangasUrl() {
        return String.format(POPULAR_MANGAS_URL, 1);
    }

    @Override
    public String getInitialSearchUrl(String query) {
        return String.format(SEARCH_URL, Uri.encode(query), 1);
    }

    @Override
    protected Request mangaDetailsRequest(String mangaUrl) {
        String mangaId = mangaUrl.substring(mangaUrl.lastIndexOf("r") + 1);
        return ReqKt.get(String.format(MANGA_URL, mangaId), requestHeaders);
    }

    @Override
    protected Request pageListRequest(String pageUrl) {
        String id = pageUrl.substring(pageUrl.indexOf("#") + 1);
        return ReqKt.get(String.format(CHAPTER_URL, id), requestHeaders);
    }

    @Override
    protected Request imageUrlRequest(Page page) {
        String pageUrl = page.getUrl();
        int start = pageUrl.indexOf("#") + 1;
        int end = pageUrl.indexOf("_", start);
        String id = pageUrl.substring(start, end);
        return ReqKt.get(String.format(PAGE_URL, id, pageUrl.substring(end+1)), requestHeaders);
    }

    private List<Manga> parseMangasFromHtml(Document parsedHtml) {
        List<Manga> mangaList = new ArrayList<>();

        if (!parsedHtml.text().contains("No (more) comics found!")) {
            for (Element currentHtmlBlock : parsedHtml.select("tr:not([id]):not([class])")) {
                Manga manga = constructMangaFromHtmlBlock(currentHtmlBlock);
                mangaList.add(manga);
            }
        }
        return mangaList;
    }

    @Override
    protected List<Manga> parsePopularMangasFromHtml(Document parsedHtml) {
        return parseMangasFromHtml(parsedHtml);
    }

    @Override
    protected String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page) {
        Element next = Parser.element(parsedHtml, "#show_more_row");
        return next != null ? String.format(POPULAR_MANGAS_URL, page.page + 1) : null;
    }

    @Override
    protected List<Manga> parseSearchFromHtml(Document parsedHtml) {
        return parseMangasFromHtml(parsedHtml);
    }

    private Manga constructMangaFromHtmlBlock(Element htmlBlock) {
        Manga manga = new Manga();
        manga.source = getId();

        Element urlElement = Parser.element(htmlBlock, "a[href^=http://bato.to]");
        if (urlElement != null) {
            manga.setUrl(urlElement.attr("href"));
            manga.title = urlElement.text().trim();
        }
        return manga;
    }

    @Override
    protected String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query) {
        Element next = Parser.element(parsedHtml, "#show_more_row");
        return next != null ? String.format(SEARCH_URL, query, page.page + 1) : null;
    }

    @Override
    protected Manga parseHtmlToManga(String mangaUrl, String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        Element tbody = parsedDocument.select("tbody").first();
        Element artistElement = tbody.select("tr:contains(Author/Artist:)").first();
        Elements genreElements = tbody.select("tr:contains(Genres:) img");

        Manga manga = Manga.create(mangaUrl);
        manga.author = Parser.text(artistElement, "td:eq(1)");
        manga.artist = Parser.text(artistElement, "td:eq(2)", manga.author);
        manga.description = Parser.text(tbody, "tr:contains(Description:) > td:eq(1)");
        manga.thumbnail_url = Parser.src(parsedDocument, "img[src^=http://img.bato.to/forums/uploads/]");
        manga.status = parseStatus(Parser.text(parsedDocument, "tr:contains(Status:) > td:eq(1)"));

        if (!genreElements.isEmpty()) {
            List<String> genres = new ArrayList<>();
            for (Element element : genreElements) {
                genres.add(element.attr("alt"));
            }
            manga.genre = TextUtils.join(", ", genres);
        }

        manga.initialized = true;
        return manga;
    }

    private int parseStatus(String status) {
        switch (status) {
            case "Ongoing":
                return Manga.ONGOING;
            case "Complete":
                return Manga.COMPLETED;
            default:
                return Manga.UNKNOWN;
        }
    }

    @Override
    protected List<Chapter> parseHtmlToChapters(String unparsedHtml) {
        Matcher matcher = staffNotice.matcher(unparsedHtml);
        if (matcher.find()) {
            String notice = Html.fromHtml(matcher.group(1)).toString().trim();
            throw new RuntimeException(notice);
        }

        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<Chapter> chapterList = new ArrayList<>();

        Elements chapterElements = parsedDocument.select("tr.row.lang_English.chapter_row");
        for (Element chapterElement : chapterElements) {
            Chapter chapter = constructChapterFromHtmlBlock(chapterElement);
            chapterList.add(chapter);
        }
        return chapterList;

    }

    private Chapter constructChapterFromHtmlBlock(Element chapterElement) {
        Chapter chapter = Chapter.create();

        Element urlElement = chapterElement.select("a[href^=http://bato.to/reader").first();
        Element dateElement = chapterElement.select("td").get(4);

        if (urlElement != null) {
            String fieldUrl = urlElement.attr("href");
            chapter.setUrl(fieldUrl);
            chapter.name = urlElement.text().trim();
        }
        if (dateElement != null) {
            chapter.date_upload = parseDateFromElement(dateElement);
        }
        return chapter;
    }

    @SuppressWarnings("WrongConstant")
    private long parseDateFromElement(Element dateElement) {
        String dateAsString = dateElement.text();

        Date date;
        try {
            date = new SimpleDateFormat("dd MMMMM yyyy - hh:mm a", Locale.ENGLISH).parse(dateAsString);
        } catch (ParseException e) {
            Matcher m = datePattern.matcher(dateAsString);

            if (m.matches()) {
                String number = m.group(1);
                int amount = number.contains("A") ? 1 : Integer.parseInt(m.group(1));
                String unit = m.group(2);

                Calendar cal = Calendar.getInstance();
                cal.add(dateFields.get(unit), -amount);
                date = cal.getTime();
            } else {
                return 0;
            }
        }
        return date.getTime();
    }

    @Override
    protected List<String> parseHtmlToPageUrls(String unparsedHtml) {
        Document parsedDocument = Jsoup.parse(unparsedHtml);

        List<String> pageUrlList = new ArrayList<>();

        Element selectElement = Parser.element(parsedDocument, "#page_select");
        if (selectElement != null) {
            for (Element pageUrlElement : selectElement.select("option")) {
                pageUrlList.add(pageUrlElement.attr("value"));
            }
        } else {
            // For webtoons in one page
            for (int i = 0; i < parsedDocument.select("div > img").size(); i++) {
                pageUrlList.add("");
            }
        }

        return pageUrlList;
    }

    @Override
    protected List<Page> parseFirstPage(List<Page> pages, String unparsedHtml) {
        if (!unparsedHtml.contains("Want to see this chapter per page instead?")) {
            String firstImage = parseHtmlToImageUrl(unparsedHtml);
            pages.get(0).setImageUrl(firstImage);
        } else {
            // For webtoons in one page
            Document parsedDocument = Jsoup.parse(unparsedHtml);
            Elements imageUrls = parsedDocument.select("div > img");
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).setImageUrl(imageUrls.get(i).attr("src"));
            }
        }
        return pages;
    }

    @Override
    protected String parseHtmlToImageUrl(String unparsedHtml) {
        int beginIndex = unparsedHtml.indexOf("<img id=\"comic_page\"");
        int endIndex = unparsedHtml.indexOf("</a>", beginIndex);
        String trimmedHtml = unparsedHtml.substring(beginIndex, endIndex);

        Document parsedDocument = Jsoup.parse(trimmedHtml);
        Element imageElement = parsedDocument.getElementById("comic_page");
        return imageElement.attr("src");
    }

    @Override
    public Observable<Boolean> login(String username, String password) {
        return networkService.requestBody(ReqKt.get(LOGIN_URL, requestHeaders))
                .flatMap(response -> doLogin(response, username, password))
                .map(this::isAuthenticationSuccessful);
    }

    private Observable<Response> doLogin(String response, String username, String password) {
        Document doc = Jsoup.parse(response);
        Element form = doc.select("#login").first();
        String postUrl = form.attr("action");

        FormBody.Builder formBody = new FormBody.Builder();
        Element authKey = form.select("input[name=auth_key]").first();

        formBody.add(authKey.attr("name"), authKey.attr("value"));
        formBody.add("ips_username", username);
        formBody.add("ips_password", password);
        formBody.add("invisible", "1");
        formBody.add("rememberMe", "1");

        return networkService.request(ReqKt.post(postUrl, requestHeaders, formBody.build()));
    }

    @Override
    protected boolean isAuthenticationSuccessful(Response response) {
        return response.priorResponse() != null && response.priorResponse().code() == 302;
    }

    @Override
    public boolean isLogged() {
        try {
            for ( HttpCookie cookie : networkService.getCookies().get(new URI(BASE_URL)) ) {
                if (cookie.getName().equals("pass_hash"))
                    return true;
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Observable<List<Chapter>> pullChaptersFromNetwork(String mangaUrl) {
        Observable<List<Chapter>> observable;
        String username = prefs.getSourceUsername(this);
        String password = prefs.getSourcePassword(this);
        if (username.isEmpty() && password.isEmpty()) {
            observable = Observable.error(new Exception("User not logged"));
        }
        else if (!isLogged()) {
            observable = login(username, password)
                    .flatMap(result -> super.pullChaptersFromNetwork(mangaUrl));
        }
        else {
            observable = super.pullChaptersFromNetwork(mangaUrl);
        }
        return observable;
    }

}
