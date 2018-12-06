/*
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package tachiyomi.ui.home

import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.bluelinelabs.conductor.Controller
import com.google.android.material.floatingactionbutton.FloatingActionButton
import tachiyomi.app.R
import tachiyomi.util.getDrawableAttr

interface HomeChildController {

  interface FAB : HomeChildController {
    fun createFAB(container: ViewGroup): FloatingActionButton
  }

  fun Controller.setupToolbarIconWithHomeController(toolbar: Toolbar) {
    val homeCtrl = parentController as? HomeController ?: return
    val homeRouter = homeCtrl.childRouter ?: return

    if (homeRouter.backstackSize != 1) {
      toolbar.navigationIcon = toolbar.context.getDrawableAttr(android.R.attr.homeAsUpIndicator)
    }
  }

}
