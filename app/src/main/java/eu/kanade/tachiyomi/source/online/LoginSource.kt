package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import okhttp3.Response
import rx.Observable

interface LoginSource : Source {

    fun isLogged(): Boolean

    fun login(username: String, password: String): Observable<Boolean>

    fun isAuthenticationSuccessful(response: Response): Boolean

}