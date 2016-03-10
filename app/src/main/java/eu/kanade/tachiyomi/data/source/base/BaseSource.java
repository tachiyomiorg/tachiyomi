package eu.kanade.tachiyomi.data.source.base;

import org.jsoup.nodes.Document;

import java.util.List;

import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.Language;
import eu.kanade.tachiyomi.data.source.model.MangasPage;
import okhttp3.Headers;
import okhttp3.Response;
import rx.Observable;

public abstract class BaseSource {

    private int id;

    // Id of the source
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public abstract Language getLang();

    // Name of the source to display
    public abstract String getName();

    // Name of the source to display with the language
    public String getVisibleName() {
        return getName() + " (" + getLang().getCode() + ")";
    }

    // Base url of the source, like: http://example.com
    public abstract String getBaseUrl();

    // True if the source requires a login
    public abstract boolean isLoginRequired();

    // Return the initial popular mangas URL
    protected abstract String getInitialPopularMangasUrl();

    // Return the initial search url given a query
    protected abstract String getInitialSearchUrl(String query);

    // Get the popular list of mangas from the source's parsed document
    protected abstract List<Manga> parsePopularMangasFromHtml(Document parsedHtml);

    // Get the next popular page URL or null if it's the last
    protected abstract String parseNextPopularMangasUrl(Document parsedHtml, MangasPage page);

    // Get the searched list of mangas from the source's parsed document
    protected abstract List<Manga> parseSearchFromHtml(Document parsedHtml);

    // Get the next search page URL or null if it's the last
    protected abstract String parseNextSearchUrl(Document parsedHtml, MangasPage page, String query);

    // Given the URL of a manga and the result of the request, return the details of the manga
    protected abstract Manga parseHtmlToManga(String mangaUrl, String unparsedHtml);

    // Given the result of the request to mangas' chapters, return a list of chapters
    protected abstract List<Chapter> parseHtmlToChapters(String unparsedHtml);

    // Given the result of the request to a chapter, return the list of URLs of the chapter
    protected abstract List<String> parseHtmlToPageUrls(String unparsedHtml);

    // Given the result of the request to a chapter's page, return the URL of the image of the page
    protected abstract String parseHtmlToImageUrl(String unparsedHtml);


    // Login related methods, shouldn't be overriden if the source doesn't require it
    public Observable<Boolean> login(String username, String password) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isLogged() {
        throw new UnsupportedOperationException("Not implemented");
    }

    protected boolean isAuthenticationSuccessful(Response response) {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Default headers, it can be overriden by children or just add new keys
    protected Headers.Builder headersBuilder() {
        Headers.Builder builder = new Headers.Builder();
        builder.add("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64)");
        return builder;
    }

    @Override
    public String toString() {
        return getVisibleName();
    }
}
