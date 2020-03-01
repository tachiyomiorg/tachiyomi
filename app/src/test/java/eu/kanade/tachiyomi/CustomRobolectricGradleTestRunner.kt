package eu.kanade.tachiyomi

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.manifest.AndroidManifest

class CustomRobolectricGradleTestRunner(klass: Class<*>) : RobolectricTestRunner(klass) {

    override fun getAppManifest(config: Config): AndroidManifest {
        return super.getAppManifest(config).apply { packageName = "eu.kanade.tachiyomi" }
    }
}
