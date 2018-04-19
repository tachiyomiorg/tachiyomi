package eu.kanade.tachiyomi.util

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

fun launchUI(block: suspend CoroutineScope.() -> Unit): Job =
        launch(UI, CoroutineStart.DEFAULT, null, block)

fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
        launch(UI, CoroutineStart.UNDISPATCHED, null, block)
