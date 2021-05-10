package eu.kanade.tachiyomi.data.track

/**
 * A TrackService that doesn't need explicit login.
 */
interface NoLoginTrackService {
    fun loginNoop()
}
