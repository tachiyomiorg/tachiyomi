/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.jaredrummler.cyanea.Cyanea
import com.jaredrummler.cyanea.app.CyaneaAppCompatActivity
import kotlinx.android.synthetic.main.main_activity.*
import tachiyomi.app.R
import tachiyomi.ui.base.withFadeTransition
import tachiyomi.ui.base.withoutTransition
import tachiyomi.ui.deeplink.ChapterDeepLinkController
import tachiyomi.ui.deeplink.MangaDeepLinkController
import tachiyomi.ui.home.HomeController

class MainActivity : CyaneaAppCompatActivity() {

  private var router: Router? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Do not let the launcher create a new activity http://stackoverflow.com/questions/16283079
    if (!isTaskRoot) {
      finish()
      return
    }

    // Init view
    setContentView(R.layout.main_activity)

    // Init conductor
    val router = Conductor.attachRouter(this, main_controller_container, savedInstanceState)
    this.router = router

    if (!router.hasRootController()) {
      // Set start screen
      rootToHomeScreen()
    }
  }

  override fun onNewIntent(intent: Intent) {
    if (!handleIntentAction(intent)) {
      super.onNewIntent(intent)
    }
  }

  private fun handleIntentAction(intent: Intent): Boolean {
    when (intent.action) {
      SHORTCUT_DEEPLINK_CHAPTER -> {
        router?.setRoot(ChapterDeepLinkController(intent.extras).withFadeTransition())
      }
      SHORTCUT_DEEPLINK_MANGA -> {
        router?.setRoot(MangaDeepLinkController(intent.extras).withFadeTransition())
      }
      else -> return false
    }
    return true
  }

  override fun onBackPressed() {
    val router = router ?: return super.onBackPressed()

    val backstackSize = router.backstackSize
    if (backstackSize == 1 && router.backstack.first().controller() !is HomeController) {
      rootToHomeScreen()
    } else if (!router.handleBack()) {
      super.onBackPressed()
    }
  }

  private fun rootToHomeScreen() {
    val router = router ?: return
    val controller = HomeController()
    val transaction = if (router.hasRootController()) {
      controller.withFadeTransition()
    } else {
      controller.withoutTransition()
    }
    router.setRoot(transaction)
  }

  companion object {
    const val SHORTCUT_DEEPLINK_MANGA = "tachiyomi.action.DEEPLINK_MANGA"
    const val SHORTCUT_DEEPLINK_CHAPTER = "tachiyomi.action.DEEPLINK_CHAPTER"
  }

}
