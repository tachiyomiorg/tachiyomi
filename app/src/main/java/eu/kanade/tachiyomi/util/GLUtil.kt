package eu.kanade.tachiyomi.util

import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import kotlin.math.max

class GLUtil private constructor() {
    companion object {
        // Safe minimum default size
        private const val IMAGE_MAX_BITMAP_DIMENSION = 2048

        val maxTextureSize: Int
            get() {
                // Get EGL Display
                val egl = EGLContext.getEGL() as EGL10
                val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                // Initialise
                val version = IntArray(2)
                egl.eglInitialize(display, version)

                // Query total number of configurations
                val totalConfigurations = IntArray(1)
                egl.eglGetConfigs(display, null, 0, totalConfigurations)

                // Query actual list configurations
                val configurationsList = arrayOfNulls<EGLConfig>(totalConfigurations[0])
                egl.eglGetConfigs(display, configurationsList, totalConfigurations[0], totalConfigurations)

                val textureSize = IntArray(1)
                var maximumTextureSize = 0

                // Iterate through all the configurations to located the maximum texture size
                for (i in 0 until totalConfigurations[0]) {
                    // Only need to check for width since opengl textures are always squared
                    egl.eglGetConfigAttrib(display, configurationsList[i], EGL10.EGL_MAX_PBUFFER_WIDTH, textureSize)

                    // Keep track of the maximum texture size
                    if (maximumTextureSize < textureSize[0]) maximumTextureSize = textureSize[0]
                }

                // Release
                egl.eglTerminate(display)

                // Return largest texture size found, or default
                return max(maximumTextureSize, IMAGE_MAX_BITMAP_DIMENSION)
            }
    }

    init {
        throw InstantiationException("This class is not for instantiation")
    }
}
