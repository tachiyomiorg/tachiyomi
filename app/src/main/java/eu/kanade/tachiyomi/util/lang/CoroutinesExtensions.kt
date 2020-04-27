package eu.kanade.tachiyomi.util.lang

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun launchUI(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT, block)

fun launchIO(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.IO, CoroutineStart.DEFAULT, block)

@OptIn(ExperimentalCoroutinesApi::class)
fun launchNow(block: suspend CoroutineScope.() -> Unit): Job =
    GlobalScope.launch(Dispatchers.Main, CoroutineStart.UNDISPATCHED, block)
