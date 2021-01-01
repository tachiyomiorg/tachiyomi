package eu.kanade.tachiyomi.ui.setting.track

import android.net.Uri
import eu.kanade.tachiyomi.util.lang.launchIO

class AnilistLoginActivity : BaseOAuthLoginActivity() {

    override fun handleResult(data: Uri?) {
        val regex = "(?:access_token=)(.*?)(?:&)".toRegex()
        val matchResult = regex.find(data?.fragment.toString())
        if (matchResult?.groups?.get(1) != null) {
            launchIO {
                trackManager.aniList.login(matchResult.groups[1]!!.value)
                returnToSettings()
            }
        } else {
            trackManager.aniList.logout()
            returnToSettings()
        }
    }
}
