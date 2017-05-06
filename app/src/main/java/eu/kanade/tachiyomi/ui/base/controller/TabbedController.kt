package eu.kanade.tachiyomi.ui.base.controller

import android.support.design.widget.TabLayout

interface TabbedController {

    fun configureTabs(tabs: TabLayout) {}

    fun cleanupTabs(tabs: TabLayout) {}
}