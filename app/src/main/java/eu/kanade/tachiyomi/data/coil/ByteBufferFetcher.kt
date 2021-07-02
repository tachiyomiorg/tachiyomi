package eu.kanade.tachiyomi.data.coil

import coil.bitmap.BitmapPool
import coil.decode.DataSource
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.size.Size
import okio.buffer
import okio.source
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer

class ByteBufferFetcher : Fetcher<ByteBuffer> {
    override suspend fun fetch(pool: BitmapPool, data: ByteBuffer, size: Size, options: Options): FetchResult {
        return SourceResult(
            source = ByteArrayInputStream(data.array()).source().buffer(),
            mimeType = null,
            dataSource = DataSource.MEMORY
        )
    }

    override fun key(data: ByteBuffer): String? = null
}
