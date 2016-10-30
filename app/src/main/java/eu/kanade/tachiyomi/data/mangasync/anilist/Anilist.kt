package eu.kanade.tachiyomi.data.mangasync.anilist

import android.content.Context
import android.graphics.Color
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.database.models.MangaSync
import eu.kanade.tachiyomi.data.mangasync.MangaSyncService
import eu.kanade.tachiyomi.data.preference.getOrDefault
import rx.Completable
import rx.Observable
import timber.log.Timber

class Anilist(private val context: Context, id: Int) : MangaSyncService(context, id) {

    companion object {
        const val READING = 1
        const val COMPLETED = 2
        const val ON_HOLD = 3
        const val DROPPED = 4
        const val PLAN_TO_READ = 5

        const val DEFAULT_STATUS = READING
        const val DEFAULT_SCORE = 0
    }

    override val name = "AniList"

    private val interceptor by lazy { AnilistInterceptor(getPassword()) }

    private val api by lazy {
        AnilistApi.createService(networkService.client.newBuilder()
                .addInterceptor(interceptor)
                .build())
    }

    override fun getLogo() = R.drawable.al

    override fun getLogoColor() = Color.rgb(18, 25, 35)

    override fun maxScore() = 100

    override fun login(username: String, password: String) = login(password)

    fun login(authCode: String): Completable {
        // Create a new api with the default client to avoid request interceptions.
        return AnilistApi.createService(client)
                // Request the access token from the API with the authorization code.
                .requestAccessToken(authCode)
                // Save the token in the interceptor.
                .doOnNext { interceptor.setAuth(it) }
                // Obtain the authenticated user from the API.
                .zipWith(api.getCurrentUser().map {
                    preferences.anilistScoreType().set(it["score_type"].asInt)
                    it["id"].toString()
                }, { oauth, user -> Pair(user, oauth.refresh_token!!) })
                // Save service credentials (username and refresh token).
                .doOnNext { saveCredentials(it.first, it.second) }
                // Logout on any error.
                .doOnError { logout() }
                .toCompletable()
    }

    override fun logout() {
        super.logout()
        interceptor.setAuth(null)
    }

    override fun search(query: String): Observable<List<MangaSync>> {
        return api.search(query, 1)
                .flatMap { Observable.from(it) }
                .filter { it.type != "Novel" }
                .map { it.toMangaSync() }
                .toList()
    }

    fun getList(): Observable<List<MangaSync>> {
        return api.getList(getUsername())
                .flatMap { Observable.from(it.flatten()) }
                .map { it.toMangaSync() }
                .toList()
    }

    override fun add(manga: MangaSync): Observable<MangaSync> {
        return api.addManga(manga.remote_id, manga.last_chapter_read, manga.getAnilistStatus())
                .doOnNext { it.body().close() }
                .doOnNext { if (!it.isSuccessful) throw Exception("Could not add manga") }
                .doOnError { Timber.e(it) }
                .map { manga }
    }

    override fun update(manga: MangaSync): Observable<MangaSync> {
        if (manga.total_chapters != 0 && manga.last_chapter_read == manga.total_chapters) {
            manga.status = COMPLETED
        }
        return api.updateManga(manga.remote_id, manga.last_chapter_read, manga.getAnilistStatus(),
                manga.getAnilistScore())
                .doOnNext { it.body().close() }
                .doOnNext { if (!it.isSuccessful) throw Exception("Could not update manga") }
                .doOnError { Timber.e(it) }
                .map { manga }
    }

    override fun bind(manga: MangaSync): Observable<MangaSync> {
        return getList()
                .flatMap { userlist ->
                    manga.sync_id = id
                    val mangaFromList = userlist.find { it.remote_id == manga.remote_id }
                    if (mangaFromList != null) {
                        manga.copyPersonalFrom(mangaFromList)
                        update(manga)
                    } else {
                        // Set default fields if it's not found in the list
                        manga.score = DEFAULT_SCORE.toFloat()
                        manga.status = DEFAULT_STATUS
                        add(manga)
                    }
                }
    }

    override fun refresh(manga: MangaSync): Observable<MangaSync> {
        return getList()
                .map { myList ->
                    val myManga = myList.find { it.remote_id == manga.remote_id }
                    if (myManga != null) {
                        manga.copyPersonalFrom(myManga)
                        manga.total_chapters = myManga.total_chapters
                        manga
                    } else {
                        throw Exception("Could not find manga")
                    }
                }
    }

    override fun getStatusList(): List<Int> {
        return listOf(READING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_READ)
    }

    override fun getStatus(status: Int): String = with(context) {
        when (status) {
            READING -> getString(R.string.reading)
            COMPLETED -> getString(R.string.completed)
            ON_HOLD -> getString(R.string.on_hold)
            DROPPED -> getString(R.string.dropped)
            PLAN_TO_READ -> getString(R.string.plan_to_read)
            else -> ""
        }
    }

    private fun MangaSync.getAnilistStatus() = when (status) {
        READING -> "reading"
        COMPLETED -> "completed"
        ON_HOLD -> "on-hold"
        DROPPED -> "dropped"
        PLAN_TO_READ -> "plan to read"
        else -> throw NotImplementedError("Unknown status")
    }

    fun MangaSync.getAnilistScore(): String = when (preferences.anilistScoreType().getOrDefault()) {
        // 10 point
        0 -> Math.floor(score.toDouble() / 10).toInt().toString()
        // 100 point
        1 -> score.toInt().toString()
        // 5 stars
        2 -> when {
            score == 0f -> "0"
            score < 30 -> "1"
            score < 50 -> "2"
            score < 70 -> "3"
            score < 90 -> "4"
            else -> "5"
        }
        // Smiley
        3 -> when {
            score == 0f -> "0"
            score <= 30 -> ":("
            score <= 60 -> ":|"
            else -> ":)"
        }
        // 10 point decimal
        4 -> (score / 10).toString()
        else -> throw Exception("Unknown score type")
    }

    override fun formatScore(manga: MangaSync): String {
        return manga.getAnilistScore()
    }

}

