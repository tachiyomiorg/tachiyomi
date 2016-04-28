package eu.kanade.tachiyomi.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlUtil {

    private static final String JPG = ".jpg";
    private static final String PNG = ".png";
    private static final String GIF = ".gif";

    private UrlUtil() throws InstantiationException {
        throw new InstantiationException("This class is not for instantiation");
    }

    public static String getPath(String s) {
        try {
            URI uri = new URI(s);
            String out = uri.getPath();
            if (uri.getQuery() != null)
                out += "?" + uri.getQuery();
            if (uri.getFragment() != null)
                out += "#" + uri.getFragment();
            return out;
        } catch (URISyntaxException e) {
            return s;
        }
    }

    public static boolean isJpg(String url) {
        return containsIgnoreCase(url, JPG);
    }

    public static boolean isPng(String url) {
        return containsIgnoreCase(url, PNG);
    }

    public static boolean isGif(String url) {
        return containsIgnoreCase(url, GIF);
    }

    public static boolean containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0)
            return true; // Empty string is contained

        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            // Quick check before calling the more expensive regionMatches() method:
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp)
                continue;

            if (src.regionMatches(true, i, what, 0, length))
                return true;
        }

        return false;
    }
}
