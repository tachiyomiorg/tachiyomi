package eu.kanade.tachiyomi.network

import android.content.Context
import android.os.Build
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class NetworkHelper(context: Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    private val cookieManager = PersistentCookieJar(context)

    val client = OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .cache(Cache(cacheDir, cacheSize))
            .enableTLS12()
            .build()

    val cloudflareClient = client.newBuilder()
            .addInterceptor(CloudflareInterceptor())
            .build()

    val cookies: PersistentCookieStore
        get() = cookieManager.store

    private fun OkHttpClient.Builder.enableTLS12(): OkHttpClient.Builder {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            return this
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.size == 1 && trustManagers[0] is X509TrustManager) {
            class TLSSocketFactory @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
            constructor() : SSLSocketFactory() {

                private val internalSSLSocketFactory: SSLSocketFactory

                init {
                    val context = SSLContext.getInstance("TLS")
                    context.init(null, null, null)
                    internalSSLSocketFactory = context.socketFactory
                }

                override fun getDefaultCipherSuites(): Array<String> {
                    return internalSSLSocketFactory.defaultCipherSuites
                }

                override fun getSupportedCipherSuites(): Array<String> {
                    return internalSSLSocketFactory.supportedCipherSuites
                }

                @Throws(IOException::class)
                override fun createSocket(): Socket? {
                    return enableTLSOnSocket(internalSSLSocketFactory.createSocket())
                }

                @Throws(IOException::class)
                override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket? {
                    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose))
                }

                @Throws(IOException::class, UnknownHostException::class)
                override fun createSocket(host: String, port: Int): Socket? {
                    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))
                }

                @Throws(IOException::class, UnknownHostException::class)
                override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket? {
                    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort))
                }

                @Throws(IOException::class)
                override fun createSocket(host: InetAddress, port: Int): Socket? {
                    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port))
                }

                @Throws(IOException::class)
                override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket? {
                    return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort))
                }

                private fun enableTLSOnSocket(socket: Socket?): Socket? {
                    if (socket != null && socket is SSLSocket) {
                        socket.enabledProtocols = socket.supportedProtocols
                    }
                    return socket
                }
            }

            sslSocketFactory(TLSSocketFactory(), trustManagers[0] as X509TrustManager)
        }

        return this
    }
}
