package eu.kanade.tachiyomi.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieStore(context: Context) {

    private val cookieMap = ConcurrentHashMap<String, List<Cookie>>()
    private val prefs = context.getSharedPreferences("cookie_store", Context.MODE_PRIVATE)

    init {
        for ((key, value) in prefs.all) {
            @Suppress("UNCHECKED_CAST")
            val cookies = value as? Set<String>
            if (cookies != null) {
                try {
                    val url = HttpUrl.parse("http://$key")
                    val nonExpiredCookies = cookies.map { Cookie.parse(url, it) }
                            .filter { !it.hasExpired() }
                    cookieMap.put(key, nonExpiredCookies)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    fun addAll(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this) {
            val key = url.uri().host

            // Append or replace the cookies for this domain.
            val cookiesForDomain = cookieMap[key].orEmpty().toMutableList()
            for (cookie in cookies) {
                // Find a cookie with the same name. Replace it if found, otherwise add a new one.
                val pos = cookiesForDomain.indexOfFirst { it.name() == cookie.name() }
                if (pos == -1) {
                    cookiesForDomain.add(cookie)
                } else {
                    cookiesForDomain[pos] = cookie
                }
            }
            cookieMap.put(key, cookiesForDomain)

            // Get cookies to be stored in disk
            val newValues = cookiesForDomain.asSequence()
                    .filter { it.persistent() && !it.hasExpired() }
                    .map { it.toString() }
                    .toSet()

            prefs.edit().putStringSet(key, newValues).apply()
        }
    }

    fun removeAll() {
        synchronized(this) {
            prefs.edit().clear().apply()
            cookieMap.clear()
        }
    }

    fun get(url: HttpUrl) = get(url.uri().host)

    fun get(uri: URI) = get(uri.host)

    private fun get(url: String): List<Cookie> {
        return cookieMap[url].orEmpty().filter { !it.hasExpired() }
    }

    private fun Cookie.hasExpired() = System.currentTimeMillis() >= expiresAt()

}