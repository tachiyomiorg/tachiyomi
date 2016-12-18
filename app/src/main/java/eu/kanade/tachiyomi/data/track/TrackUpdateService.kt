package eu.kanade.tachiyomi.data.track

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import eu.kanade.tachiyomi.data.database.DatabaseHelper
import eu.kanade.tachiyomi.data.database.models.Track
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.injectLazy

class TrackUpdateService : Service() {

    val trackManager: TrackManager by injectLazy()
    val db: DatabaseHelper by injectLazy()

    private lateinit var subscriptions: CompositeSubscription

    override fun onCreate() {
        super.onCreate()
        subscriptions = CompositeSubscription()
    }

    override fun onDestroy() {
        subscriptions.unsubscribe()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val track = intent.getSerializableExtra(EXTRA_TRACK)
        if (track != null) {
            updateLastChapterRead(track as Track, startId)
            return Service.START_REDELIVER_INTENT
        } else {
            stopSelf(startId)
            return Service.START_NOT_STICKY
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun updateLastChapterRead(track: Track, startId: Int) {
        val sync = trackManager.getService(track.sync_id)
        if (sync == null) {
            stopSelf(startId)
            return
        }

        subscriptions.add(Observable.defer { sync.update(track) }
                .flatMap { db.insertTrack(track).asRxObservable() }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ stopSelf(startId) },
                        { stopSelf(startId) }))
    }

    companion object {

        private val EXTRA_TRACK = "extra_track"

        @JvmStatic
        fun start(context: Context, track: Track) {
            val intent = Intent(context, TrackUpdateService::class.java)
            intent.putExtra(EXTRA_TRACK, track)
            context.startService(intent)
        }
    }

}
