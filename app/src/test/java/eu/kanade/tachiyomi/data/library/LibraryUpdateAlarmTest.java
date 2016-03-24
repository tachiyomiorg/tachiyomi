package eu.kanade.tachiyomi.data.library;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPendingIntent;

import eu.kanade.tachiyomi.BuildConfig;
import eu.kanade.tachiyomi.CustomRobolectricGradleTestRunner;
import eu.kanade.tachiyomi.data.preference.PreferencesHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.LOLLIPOP)
@RunWith(CustomRobolectricGradleTestRunner.class)
public class LibraryUpdateAlarmTest {

    ShadowApplication app;
    Context context;
    ShadowAlarmManager alarmManager;

    @Before
    public void setup() {
        app = ShadowApplication.getInstance();
        context = spy(app.getApplicationContext());

        alarmManager = shadowOf((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    }

    @Test
    public void testLibraryIntentHandling() {
        Intent intent = new Intent(LibraryUpdateAlarm.LIBRARY_UPDATE_ACTION);
        assertThat(app.hasReceiverForIntent(intent)).isTrue();
    }

    @Test
    public void testAlarmIsNotStarted() {
        assertThat(alarmManager.getNextScheduledAlarm()).isNull();
    }

    @Test
    public void testAlarmIsNotStartedWhenBootReceivedAndSettingZero() {
        LibraryUpdateAlarm alarm = new LibraryUpdateAlarm();
        alarm.onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));

        assertThat(alarmManager.getNextScheduledAlarm()).isNull();
    }

    @Test
    public void testAlarmIsStartedWhenBootReceivedAndSettingNotZero() {
        PreferencesHelper prefs = new PreferencesHelper(context);
        prefs.libraryUpdateInterval().set(1);

        LibraryUpdateAlarm alarm = new LibraryUpdateAlarm();
        alarm.onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));

        assertThat(alarmManager.getNextScheduledAlarm()).isNotNull();
    }

    @Test
    public void testOnlyOneAlarmExists() {
        PreferencesHelper prefs = new PreferencesHelper(context);
        prefs.libraryUpdateInterval().set(1);

        LibraryUpdateAlarm.startAlarm(context);
        LibraryUpdateAlarm.startAlarm(context);
        LibraryUpdateAlarm.startAlarm(context);

        assertThat(alarmManager.getScheduledAlarms()).hasSize(1);
    }

    @Test
    public void testLibraryWillBeUpdatedWhenAlarmFired() {
        PreferencesHelper prefs = new PreferencesHelper(context);
        prefs.libraryUpdateInterval().set(1);

        Intent expectedIntent = new Intent(context, LibraryUpdateAlarm.class);
        expectedIntent.setAction(LibraryUpdateAlarm.LIBRARY_UPDATE_ACTION);

        LibraryUpdateAlarm.startAlarm(context);

        ShadowAlarmManager.ScheduledAlarm scheduledAlarm = alarmManager.getNextScheduledAlarm();
        ShadowPendingIntent pendingIntent = shadowOf(scheduledAlarm.operation);
        assertThat(pendingIntent.isBroadcastIntent()).isTrue();
        assertThat(pendingIntent.getSavedIntents()).hasSize(1);
        assertThat(expectedIntent.getComponent()).isEqualTo(pendingIntent.getSavedIntents()[0].getComponent());
        assertThat(expectedIntent.getAction()).isEqualTo(pendingIntent.getSavedIntents()[0].getAction());
    }

    @Test
    public void testLibraryUpdateServiceIsStartedWhenUpdateIntentIsReceived() {
        Intent intent = new Intent(context, LibraryUpdateService.class);
        intent.putExtra("is_forced", false);
        assertThat(app.getNextStartedService()).isNotEqualTo(intent);

        LibraryUpdateAlarm alarm = new LibraryUpdateAlarm();
        alarm.onReceive(context, new Intent(LibraryUpdateAlarm.LIBRARY_UPDATE_ACTION));

        assertThat(app.getNextStartedService()).isEqualTo(intent);
    }

    @Test
    public void testReceiverDoesntReactToNullActions() {
        PreferencesHelper prefs = new PreferencesHelper(context);
        prefs.libraryUpdateInterval().set(1);

        Intent intent = new Intent(context, LibraryUpdateService.class);

        LibraryUpdateAlarm alarm = new LibraryUpdateAlarm();
        alarm.onReceive(context, new Intent());

        assertThat(app.getNextStartedService()).isNotEqualTo(intent);
        assertThat(alarmManager.getScheduledAlarms()).hasSize(0);
    }

    @Test
    public void testAlarmFiresCloseToDesiredTime() {
        int hours = 2;
        LibraryUpdateAlarm.startAlarm(context, hours);

        long shouldRunAt = SystemClock.elapsedRealtime() + (hours * 60 * 60 * 1000);

        // Margin error of 3 seconds
        assertThat(alarmManager.getNextScheduledAlarm().triggerAtTime)
                .isGreaterThan(shouldRunAt - 3000)
                .isLessThan(shouldRunAt + 3000);
    }

}
