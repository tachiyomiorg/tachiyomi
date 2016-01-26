package eu.kanade.tachiyomi.data.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.f2prateek.rx.preferences.Preference;
import com.f2prateek.rx.preferences.RxSharedPreferences;

import java.io.File;
import java.io.IOException;

import eu.kanade.tachiyomi.R;
import eu.kanade.tachiyomi.data.mangasync.base.MangaSyncService;
import eu.kanade.tachiyomi.data.source.base.Source;

public class PreferencesHelper {

    private Context context;
    private SharedPreferences prefs;
    private RxSharedPreferences rxPrefs;

    private static final String SOURCE_ACCOUNT_USERNAME = "pref_source_username_";
    private static final String SOURCE_ACCOUNT_PASSWORD = "pref_source_password_";
    private static final String MANGASYNC_ACCOUNT_USERNAME = "pref_mangasync_username_";
    private static final String MANGASYNC_ACCOUNT_PASSWORD = "pref_mangasync_password_";

    private File defaultDownloadsDir;

    public PreferencesHelper(Context context) {
        this.context = context;
        PreferenceManager.setDefaultValues(context, R.xml.pref_reader, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        rxPrefs = RxSharedPreferences.create(prefs);

        defaultDownloadsDir = new File(Environment.getExternalStorageDirectory() +
                File.separator + context.getString(R.string.app_name), "downloads");

        // Create default directory
        if (getDownloadsDirectory().equals(defaultDownloadsDir.getAbsolutePath()) &&
                !defaultDownloadsDir.exists()) {
            defaultDownloadsDir.mkdirs();
            try {
                new File(defaultDownloadsDir, ".nomedia").createNewFile();
            } catch (IOException e) { /* Ignore */ }
        }
    }

    private String getKey(int keyResource) {
        return context.getString(keyResource);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    public Preference<Boolean> lockOrientation() {
        return rxPrefs.getBoolean(getKey(R.string.pref_lock_orientation_key), true);
    }

    public Preference<Boolean> enableTransitions() {
        return rxPrefs.getBoolean(getKey(R.string.pref_enable_transitions_key), true);
    }

    public Preference<Boolean> showPageNumber() {
        return rxPrefs.getBoolean(getKey(R.string.pref_show_page_number_key), true);
    }

    public Preference<Boolean> hideStatusBar() {
        return rxPrefs.getBoolean(getKey(R.string.pref_hide_status_bar_key), true);
    }

    public Preference<Boolean> keepScreenOn() {
        return rxPrefs.getBoolean(getKey(R.string.pref_keep_screen_on_key), true);
    }

    public Preference<Boolean> customBrightness() {
        return rxPrefs.getBoolean(getKey(R.string.pref_custom_brightness_key), false);
    }

    public Preference<Float> customBrightnessValue() {
        return rxPrefs.getFloat(getKey(R.string.pref_custom_brightness_value_key), 0F);
    }

    public int getDefaultViewer() {
        return prefs.getInt(getKey(R.string.pref_default_viewer_key), 1);
    }

    public Preference<Integer> portraitColumns() {
        return rxPrefs.getInteger(getKey(R.string.pref_library_columns_portrait_key), 0);
    }

    public Preference<Integer> landscapeColumns() {
        return rxPrefs.getInteger(getKey(R.string.pref_library_columns_landscape_key), 0);
    }

    public boolean updateOnlyNonCompleted() {
        return prefs.getBoolean(getKey(R.string.pref_update_only_non_completed_key), false);
    }

    public boolean autoUpdateMangaSync() {
        return prefs.getBoolean(getKey(R.string.pref_auto_update_manga_sync_key), true);
    }

    public boolean askUpdateMangaSync() {
        return prefs.getBoolean(getKey(R.string.pref_ask_update_manga_sync_key), false);
    }

    public Preference<Integer> imageDecoder() {
        return rxPrefs.getInteger(getKey(R.string.pref_image_decoder_key), 0);
    }

    public Preference<Integer> readerTheme() {
        return rxPrefs.getInteger(getKey(R.string.pref_reader_theme_key), 0);
    }

    public Preference<Boolean> catalogueAsList() {
        return rxPrefs.getBoolean(getKey(R.string.pref_display_catalogue_as_list), false);
    }

    public String getSourceUsername(Source source) {
        return prefs.getString(SOURCE_ACCOUNT_USERNAME + source.getId(), "");
    }

    public String getSourcePassword(Source source) {
        return prefs.getString(SOURCE_ACCOUNT_PASSWORD + source.getId(), "");
    }

    public void setSourceCredentials(Source source, String username, String password) {
        prefs.edit()
                .putString(SOURCE_ACCOUNT_USERNAME + source.getId(), username)
                .putString(SOURCE_ACCOUNT_PASSWORD + source.getId(), password)
                .apply();
    }

    public String getMangaSyncUsername(MangaSyncService sync) {
        return prefs.getString(MANGASYNC_ACCOUNT_USERNAME + sync.getId(), "");
    }

    public String getMangaSyncPassword(MangaSyncService sync) {
        return prefs.getString(MANGASYNC_ACCOUNT_PASSWORD + sync.getId(), "");
    }

    public void setMangaSyncCredentials(MangaSyncService sync, String username, String password) {
        prefs.edit()
                .putString(MANGASYNC_ACCOUNT_USERNAME + sync.getId(), username)
                .putString(MANGASYNC_ACCOUNT_PASSWORD + sync.getId(), password)
                .apply();
    }

    public String getDownloadsDirectory() {
        return prefs.getString(getKey(R.string.pref_download_directory_key),
                defaultDownloadsDir.getAbsolutePath());
    }

    public void setDownloadsDirectory(String path) {
        prefs.edit().putString(getKey(R.string.pref_download_directory_key), path).apply();
    }

    public Preference<Integer> downloadThreads() {
        return rxPrefs.getInteger(getKey(R.string.pref_download_slots_key), 1);
    }

    public boolean downloadOnlyOverWifi() {
        return prefs.getBoolean(getKey(R.string.pref_download_only_over_wifi_key), true);
    }

    public static int getLibraryUpdateInterval(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(
                context.getString(R.string.pref_library_update_interval_key), 0);
    }

}
