package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.alpha
import androidx.core.graphics.blue
import androidx.core.graphics.createBitmap
import androidx.core.graphics.green
import androidx.core.graphics.red
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLConnection
import kotlin.math.abs

object ImageUtil {

    fun isImage(name: String, openStream: (() -> InputStream)? = null): Boolean {
        val contentType = try {
            URLConnection.guessContentTypeFromName(name)
        } catch (e: Exception) {
            null
        } ?: openStream?.let { findImageType(it)?.mime }
        return contentType?.startsWith("image/") ?: false
    }

    fun findImageType(openStream: () -> InputStream): ImageType? {
        return openStream().use { findImageType(it) }
    }

    fun findImageType(stream: InputStream): ImageType? {
        try {
            val bytes = ByteArray(8)

            val length = if (stream.markSupported()) {
                stream.mark(bytes.size)
                stream.read(bytes, 0, bytes.size).also { stream.reset() }
            } else {
                stream.read(bytes, 0, bytes.size)
            }

            if (length == -1) {
                return null
            }

            if (bytes.compareWith(charByteArrayOf(0xFF, 0xD8, 0xFF))) {
                return ImageType.JPG
            }
            if (bytes.compareWith(charByteArrayOf(0x89, 0x50, 0x4E, 0x47))) {
                return ImageType.PNG
            }
            if (bytes.compareWith("GIF8".toByteArray())) {
                return ImageType.GIF
            }
            if (bytes.compareWith("RIFF".toByteArray())) {
                return ImageType.WEBP
            }
        } catch (e: Exception) {
        }
        return null
    }

    private fun ByteArray.compareWith(magic: ByteArray): Boolean {
        return magic.indices.none { this[it] != magic[it] }
    }

    private fun charByteArrayOf(vararg bytes: Int): ByteArray {
        return ByteArray(bytes.size).apply {
            for (i in bytes.indices) {
                set(i, bytes[i].toByte())
            }
        }
    }

    enum class ImageType(val mime: String, val extension: String) {
        JPG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        GIF("image/gif", "gif"),
        WEBP("image/webp", "webp")
    }

    /**
     * Check whether the image is a double-page spread
     * @return true if the width is greater than the height
     */
    fun isDoublePage(imageStream: InputStream): Boolean {
        imageStream.mark(imageStream.available() + 1)

        val imageBytes = imageStream.readBytes()

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        imageStream.reset()

        return options.outWidth > options.outHeight
    }

    /**
     * Extract the 'side' part from imageStream and return it as InputStream.
     */
    fun splitInHalf(imageStream: InputStream, side: Side): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val height = imageBitmap.height
        val width = imageBitmap.width

        val singlePage = Rect(0, 0, width / 2, height)

        val half = createBitmap(width / 2, height)
        val part = when (side) {
            Side.RIGHT -> Rect(width - width / 2, 0, width, height)
            Side.LEFT -> Rect(0, 0, width / 2, height)
        }
        val canvas = Canvas(half)
        canvas.drawBitmap(imageBitmap, part, singlePage, null)
        val output = ByteArrayOutputStream()
        half.compress(Bitmap.CompressFormat.JPEG, 100, output)

        return ByteArrayInputStream(output.toByteArray())
    }

    /**
     * Split the image into left and right parts, then merge them into a new image.
     */
    fun splitAndMerge(imageStream: InputStream, upperSide: Side): InputStream {
        val imageBytes = imageStream.readBytes()

        val imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val height = imageBitmap.height
        val width = imageBitmap.width

        val result = createBitmap(width / 2, height * 2)
        val canvas = Canvas(result)
        // right -> upper
        val rightPart = when (upperSide) {
            Side.RIGHT -> Rect(width - width / 2, 0, width, height)
            Side.LEFT -> Rect(0, 0, width / 2, height)
        }
        val upperPart = Rect(0, 0, width / 2, height)
        canvas.drawBitmap(imageBitmap, rightPart, upperPart, null)
        // left -> bottom
        val leftPart = when (upperSide) {
            Side.LEFT -> Rect(width - width / 2, 0, width, height)
            Side.RIGHT -> Rect(0, 0, width / 2, height)
        }
        val bottomPart = Rect(0, height, width / 2, height * 2)
        canvas.drawBitmap(imageBitmap, leftPart, bottomPart, null)

        val output = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 100, output)
        return ByteArrayInputStream(output.toByteArray())
    }

    enum class Side {
        RIGHT, LEFT
    }

    fun chooseBackground(context: Context, imageStream: InputStream): Drawable {
        imageStream.mark(imageStream.available() + 1)

        val image = BitmapFactory.decodeStream(imageStream)

        imageStream.reset()

        val whiteColor = Color.WHITE
        if (image == null) return ColorDrawable(whiteColor)
        if (image.width < 50 || image.height < 50) {
            return ColorDrawable(whiteColor)
        }

        val top = 5
        val bot = image.height - 5
        val left = (image.width * 0.0275).toInt()
        val right = image.width - left
        val midX = image.width / 2
        val midY = image.height / 2
        val offsetX = (image.width * 0.01).toInt()

        val topLeftPixel = image.getPixel(left, top)
        val topRightPixel = image.getPixel(right, top)
        val midLeftPixel = image.getPixel(left, midY)
        val midRightPixel = image.getPixel(right, midY)
        val topCenterPixel = image.getPixel(midX, top)
        val botLeftPixel = image.getPixel(left, bot)
        val bottomCenterPixel = image.getPixel(midX, bot)
        val botRightPixel = image.getPixel(right, bot)

        val topLeftIsDark = topLeftPixel.isDark()
        val topRightIsDark = topRightPixel.isDark()
        val midLeftIsDark = midLeftPixel.isDark()
        val midRightIsDark = midRightPixel.isDark()
        val topMidIsDark = topCenterPixel.isDark()
        val botLeftIsDark = botLeftPixel.isDark()
        val botRightIsDark = botRightPixel.isDark()

        var darkBG = (topLeftIsDark && (botLeftIsDark || botRightIsDark || topRightIsDark || midLeftIsDark || topMidIsDark)) ||
            (topRightIsDark && (botRightIsDark || botLeftIsDark || midRightIsDark || topMidIsDark))

        if (!topLeftPixel.isWhite() && topLeftPixel.isCloseTo(topCenterPixel) &&
            !topCenterPixel.isWhite() && topCenterPixel.isCloseTo(topRightPixel) &&
            !topRightPixel.isWhite() && topRightPixel.isCloseTo(botRightPixel) &&
            !botRightPixel.isWhite() && botRightPixel.isCloseTo(bottomCenterPixel) &&
            !bottomCenterPixel.isWhite() && bottomCenterPixel.isCloseTo(botLeftPixel) &&
            !botLeftPixel.isWhite() && botLeftPixel.isCloseTo(topLeftPixel)
        ) {
            return ColorDrawable(topLeftPixel)
        }

        val whiteCorners = listOf(
            topLeftPixel.isWhite(),
            topRightPixel.isWhite(),
            botLeftPixel.isWhite(),
            botRightPixel.isWhite()
        ).filter { it }

        if (whiteCorners.size > 2) {
            darkBG = false
        }

        var blackColor = when {
            topLeftIsDark -> topLeftPixel
            topRightIsDark -> topRightPixel
            botLeftIsDark -> botLeftPixel
            botRightIsDark -> botRightPixel
            else -> whiteColor
        }

        var overallWhitePixels = 0
        var overallBlackPixels = 0
        var topBlackStreak = 0
        var topWhiteStreak = 0
        var botBlackStreak = 0
        var botWhiteStreak = 0
        outer@ for (x in intArrayOf(left, right, left - offsetX, right + offsetX)) {
            var whitePixelsStreak = 0
            var whitePixels = 0
            var blackPixelsStreak = 0
            var blackPixels = 0
            var blackStreak = false
            var whiteStreak = false
            val notOffset = x == left || x == right
            for ((index, y) in (0 until image.height step image.height / 25).withIndex()) {
                val pixel = image.getPixel(x, y)
                val pixelOff = image.getPixel(x + (if (x < image.width / 2) -offsetX else offsetX), y)
                if (pixel.isWhite()) {
                    whitePixelsStreak++
                    whitePixels++
                    if (notOffset) {
                        overallWhitePixels++
                    }
                    if (whitePixelsStreak > 14) {
                        whiteStreak = true
                    }
                    if (whitePixelsStreak > 6 && whitePixelsStreak >= index - 1) {
                        topWhiteStreak = whitePixelsStreak
                    }
                } else {
                    whitePixelsStreak = 0
                    if (pixel.isDark() && pixelOff.isDark()) {
                        blackPixels++
                        if (notOffset) {
                            overallBlackPixels++
                        }
                        blackPixelsStreak++
                        if (blackPixelsStreak >= 14) {
                            blackStreak = true
                        }
                        continue
                    }
                }
                if (blackPixelsStreak > 6 && blackPixelsStreak >= index - 1) {
                    topBlackStreak = blackPixelsStreak
                }
                blackPixelsStreak = 0
            }
            if (blackPixelsStreak > 6) {
                botBlackStreak = blackPixelsStreak
            } else if (whitePixelsStreak > 6) {
                botWhiteStreak = whitePixelsStreak
            }
            when {
                blackPixels > 22 -> {
                    if (x == right || x == right + offsetX) {
                        blackColor = when {
                            topRightIsDark -> topRightPixel
                            botRightIsDark -> botRightPixel
                            else -> blackColor
                        }
                    }
                    darkBG = true
                    overallWhitePixels = 0
                    break@outer
                }
                blackStreak -> {
                    darkBG = true
                    if (x == right || x == right + offsetX) {
                        blackColor = when {
                            topRightIsDark -> topRightPixel
                            botRightIsDark -> botRightPixel
                            else -> blackColor
                        }
                    }
                    if (blackPixels > 18) {
                        overallWhitePixels = 0
                        break@outer
                    }
                }
                whiteStreak || whitePixels > 22 -> darkBG = false
            }
        }

        val topIsBlackStreak = topBlackStreak > topWhiteStreak
        val bottomIsBlackStreak = botBlackStreak > botWhiteStreak
        if (overallWhitePixels > 9 && overallWhitePixels > overallBlackPixels) {
            darkBG = false
        }
        if (topIsBlackStreak && bottomIsBlackStreak) {
            darkBG = true
        }

        val isLandscape = context.resources.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            return when {
                darkBG -> ColorDrawable(blackColor)
                else -> ColorDrawable(whiteColor)
            }
        }

        val gradient = when {
            darkBG && botLeftPixel.isWhite() && botRightPixel.isWhite() -> {
                intArrayOf(blackColor, blackColor, whiteColor, whiteColor)
            }
            darkBG && topLeftPixel.isWhite() && topRightPixel.isWhite() -> {
                intArrayOf(whiteColor, whiteColor, blackColor, blackColor)
            }
            darkBG -> {
                return ColorDrawable(blackColor)
            }
            topIsBlackStreak || (
                topLeftIsDark && topRightIsDark &&
                    image.getPixel(left - offsetX, top).isDark() && image.getPixel(right + offsetX, top).isDark() &&
                    (topMidIsDark || overallBlackPixels > 9)
                ) -> {
                intArrayOf(blackColor, blackColor, whiteColor, whiteColor)
            }
            bottomIsBlackStreak || (
                botLeftIsDark && botRightIsDark &&
                    image.getPixel(left - offsetX, bot).isDark() && image.getPixel(right + offsetX, bot).isDark() &&
                    (bottomCenterPixel.isDark() || overallBlackPixels > 9)
                ) -> {
                intArrayOf(whiteColor, whiteColor, blackColor, blackColor)
            }
            else -> {
                return ColorDrawable(whiteColor)
            }
        }

        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            gradient
        )
    }

    private fun Int.isDark(): Boolean =
        red < 40 && blue < 40 && green < 40 && alpha > 200

    private fun Int.isCloseTo(other: Int): Boolean =
        abs(red - other.red) < 30 && abs(green - other.green) < 30 && abs(blue - other.blue) < 30

    private fun Int.isWhite(): Boolean =
        red + blue + green > 740
}
