package eu.kanade.tachiyomi.data.library;

import android.content.Context;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

import eu.kanade.tachiyomi.BuildConfig;
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner;
import eu.kanade.tachiyomi.data.database.models.Chapter;
import eu.kanade.tachiyomi.data.database.models.Manga;
import eu.kanade.tachiyomi.data.source.base.OnlineSource;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(CustomRobolectricGradleTestRunner.class)
public class LibraryUpdateServiceTest {

    ShadowApplication app;
    Context context;
    LibraryUpdateService service;
    OnlineSource source;

    @Before
    public void setup() {
        app = ShadowApplication.getInstance();
        context = app.getApplicationContext();
        service = Robolectric.setupService(LibraryUpdateService.class);
        source = mock(OnlineSource.class);
        when(service.sourceManager.get(anyInt())).thenReturn(source);
    }

    @Test
    public void testLifecycle() {
        // Smoke test
        Robolectric.buildService(LibraryUpdateService.class)
                .attach()
                .create()
                .startCommand(0, 0)
                .destroy()
                .get();
    }

    @Test
    public void testUpdateManga() {
        Manga manga = createManga("/manga1").get(0);
        manga.id = 1L;
        service.db.insertManga(manga).executeAsBlocking();

        List<Chapter> sourceChapters = createChapters("/chapter1", "/chapter2");

        when(source.fetchChapterList(manga)).thenReturn(Observable.just(sourceChapters));

        service.updateManga(manga).subscribe();

        assertThat(service.db.getChapters(manga).executeAsBlocking()).hasSize(2);
    }

    @Test
    public void testContinuesUpdatingWhenAMangaFails() {
        List<Manga> favManga = createManga("/manga1", "/manga2", "/manga3");
        service.db.insertMangas(favManga).executeAsBlocking();
        favManga = service.db.getFavoriteMangas().executeAsBlocking();

        List<Chapter> chapters = createChapters("/chapter1", "/chapter2");
        List<Chapter> chapters3 = createChapters("/achapter1", "/achapter2");

        // One of the updates will fail
        when(source.fetchChapterList(favManga.get(0))).thenReturn(Observable.just(chapters));
        when(source.fetchChapterList(favManga.get(1))).thenReturn(Observable.<List<Chapter>>error(new Exception()));
        when(source.fetchChapterList(favManga.get(2))).thenReturn(Observable.just(chapters3));

        service.updateMangaList(service.getMangaToUpdate(null)).subscribe();

        // There are 3 network attempts and 2 insertions (1 request failed)
        assertThat(service.db.getChapters(favManga.get(0)).executeAsBlocking()).hasSize(2);
        assertThat(service.db.getChapters(favManga.get(1)).executeAsBlocking()).hasSize(0);
        assertThat(service.db.getChapters(favManga.get(2)).executeAsBlocking()).hasSize(2);
    }

    private List<Chapter> createChapters(String... urls) {
        List<Chapter> list = new ArrayList<>();
        for (String url : urls) {
            Chapter c = Chapter.create();
            c.url = url;
            c.name = url.substring(1);
            list.add(c);
        }
        return list;
    }

    private List<Manga> createManga(String... urls) {
        List<Manga> list = new ArrayList<>();
        for (String url : urls) {
            Manga m = Manga.create(url);
            m.title = url.substring(1);
            m.favorite = true;
            list.add(m);
        }
        return list;
    }
}
