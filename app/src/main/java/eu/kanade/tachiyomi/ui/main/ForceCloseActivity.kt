package eu.kanade.tachiyomi.ui.main

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity that in conjunction with its configuration in the manifest allows for a way to
 * "force close" the application from the main [App] class.
 */
class ForceCloseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

    companion object {
        fun closeApp(context: Context) {
            val intent = Intent(context, ForceCloseActivity::class.java).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }
}
