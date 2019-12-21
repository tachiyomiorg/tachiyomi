package eu.kanade.tachiyomi.network

import android.content.Context
import android.os.Build
import okhttp3.*
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.security.KeyManagementException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import javax.net.ssl.*

class NetworkHelper(context: Context) {

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar(context)

    val client = OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .cache(Cache(cacheDir, cacheSize))
            .enableTLS12()
            .build()

    val cloudflareClient = client.newBuilder()
            .addInterceptor(CloudflareInterceptor(context))
            .build()

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

        val specCompat = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0)
            .cipherSuites(
                    *ConnectionSpec.MODERN_TLS.cipherSuites().orEmpty().toTypedArray(),
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
            )
            .build()

        val specs = listOf(specCompat, ConnectionSpec.CLEARTEXT)
        connectionSpecs(specs)

        return this
    }
}
