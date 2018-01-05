package eu.kanade.tachiyomi.data.sync.api

import android.accounts.Account
import eu.kanade.tachiyomi.data.sync.api.models.AuthResponse
import eu.kanade.tachiyomi.data.sync.api.models.TestAuthenticatedResponse
import eu.kanade.tachiyomi.data.sync.gson.SyncGsonProvider
import eu.kanade.tachiyomi.data.sync.protocol.models.SyncReport
import eu.kanade.tachiyomi.data.sync.protocol.models.common.SyncResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import rx.Observable
import java.util.concurrent.TimeUnit

/**
 * TachiWeb Retrofit API
 */

interface TWApi {
    /**
     * Get an token for a password
     */
    @GET("auth")
    fun checkAuth(@Query("password") password: String): Observable<AuthResponse>
    
    /**
     * Submit a sync report to the server and receive a new sync report back
     */
    @POST("sync")
    fun sync(@Header("TW-Session") token: String, @Body diff: SyncReport): Observable<SyncResponse>
    
    /**
     * Test whether or not an auth token is valid
     */
    @GET("test_auth")
    fun testAuthenticated(@Header("TW-Session") token: String): Observable<TestAuthenticatedResponse>
    
    companion object {
        fun create(client: OkHttpClient, baseUrl: String) = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(SyncGsonProvider.gson))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build()
                .create(TWApi::class.java)!!
        
        fun apiFromAccount(account: Account)
                = TWApi.create(OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.SECONDS)
                //Give server plenty of time to generate report
                .readTimeout(5, TimeUnit.MINUTES)
                .writeTimeout(5, TimeUnit.MINUTES)
                .build(), account.name)
    }
}