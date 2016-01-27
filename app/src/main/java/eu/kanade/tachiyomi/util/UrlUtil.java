package eu.kanade.tachiyomi.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtil {

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
}
