package eu.kanade.tachiyomi.util;

import android.support.annotation.Nullable;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class Parser {

    private Parser() throws InstantiationException {
        throw new InstantiationException("This class is not for instantiation");
    }

    @Nullable
    public static Element element(Element container, String pattern) {
        return container.select(pattern).first();
    }

    @Nullable
    public static String text(Element container, String pattern) {
        return text(container, pattern, null);
    }

    @Nullable
    public static String text(Element container, String pattern, String defValue) {
        Element element = container.select(pattern).first();
        return element != null ? element.text() : defValue;
    }

    @Nullable
    public static String allText(Element container, String pattern) {
        Elements elements = container.select(pattern);
        return !elements.isEmpty() ? elements.text() : null;
    }

    @Nullable
    public static String attr(Element container, String pattern, String attr) {
        Element element = container.select(pattern).first();
        return element != null ? element.attr(attr) : null;
    }

    @Nullable
    public static String href(Element container, String pattern) {
        return attr(container, pattern, "href");
    }

    @Nullable
    public static String src(Element container, String pattern) {
        return attr(container, pattern, "src");
    }

}
