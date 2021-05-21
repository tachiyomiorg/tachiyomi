package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Resources

object InternalResourceHelper {

    fun getBoolean(context: Context, resName: String, defaultValue: Boolean): Boolean {
        val id = getResourceId(resName, "bool")
        return if (id != 0) {
            context.createPackageContext("android", 0).resources.getBoolean(id)
        } else {
            defaultValue
        }
    }

    /**
     * Get resource id from system resources
     * @param resName resource name to get
     * @param type resource type of [resName] to get
     * @return 0 if not available
     */
    private fun getResourceId(resName: String, type: String): Int {
        return Resources.getSystem().getIdentifier(resName, type, "android")
    }
}
