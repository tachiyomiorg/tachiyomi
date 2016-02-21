package eu.kanade.tachiyomi.data.source;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import eu.kanade.tachiyomi.data.source.base.Source;
import eu.kanade.tachiyomi.data.source.online.english.Batoto;
import eu.kanade.tachiyomi.data.source.online.english.Kissmanga;
import eu.kanade.tachiyomi.data.source.online.english.Mangafox;
import eu.kanade.tachiyomi.data.source.online.english.Mangahere;
import eu.kanade.tachiyomi.data.source.online.russian.Readmanga;
import eu.kanade.tachiyomi.data.source.online.russian.Mintmanga;
import eu.kanade.tachiyomi.data.source.online.russian.Mangachan;

public class SourceManager {

    public static final int BATOTO = 1;
    public static final int MANGAHERE = 2;
    public static final int MANGAFOX = 3;
    public static final int KISSMANGA = 4;
    public static final int READMANGA = 5;
    public static final int MINTMANGA = 6;
    public static final int MANGACHAN = 7;


    private HashMap<Integer, Source> sourcesMap;
    private Context context;

    public SourceManager(Context context) {
        sourcesMap = new HashMap<>();
        this.context = context;

        initializeSources();
    }

    public Source get(int sourceKey) {
        if (!sourcesMap.containsKey(sourceKey)) {
            sourcesMap.put(sourceKey, createSource(sourceKey));
        }
        return sourcesMap.get(sourceKey);
    }

    private Source createSource(int sourceKey) {
        switch (sourceKey) {
            case BATOTO:
                return new Batoto(context);
            case MANGAHERE:
                return new Mangahere(context);
            case MANGAFOX:
                return new Mangafox(context);
            case KISSMANGA:
                return new Kissmanga(context);
            case READMANGA:
                return new Readmanga(context);
            case MINTMANGA:
                return new Mintmanga(context);
            case MANGACHAN:
                return new Mangachan(context);
        }

        return null;
    }

    private void initializeSources() {
        sourcesMap.put(BATOTO, createSource(BATOTO));
        sourcesMap.put(MANGAHERE, createSource(MANGAHERE));
        sourcesMap.put(MANGAFOX, createSource(MANGAFOX));
        sourcesMap.put(KISSMANGA, createSource(KISSMANGA));
        sourcesMap.put(READMANGA, createSource(READMANGA));
        sourcesMap.put(MINTMANGA, createSource(MINTMANGA));
        sourcesMap.put(MANGACHAN, createSource(MANGACHAN));
    }

    public List<Source> getSources() {
        List<Source> sources = new ArrayList<>(sourcesMap.values());
        Collections.sort(sources, (s1, s2) -> s1.getName().compareTo(s2.getName()));
        return sources;
    }

}
