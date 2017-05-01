package eu.kanade.tachiyomi.ui.base.controller

import android.support.v4.widget.DrawerLayout
import android.view.ViewGroup

interface SecondaryDrawerController {

    fun createSecondaryDrawer(drawer: DrawerLayout): ViewGroup?

    fun cleanupSecondaryDrawer(drawer: DrawerLayout)
}