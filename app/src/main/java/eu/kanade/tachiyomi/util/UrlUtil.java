package eu.kanade.tachiyomi.util;

import java.net.URI;
import java.net.URISyntaxException;

public final class UrlUtil {

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

}
