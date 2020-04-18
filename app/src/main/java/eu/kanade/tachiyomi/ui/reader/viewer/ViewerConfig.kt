package eu.kanade.tachiyomi.ui.reader.viewer

import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.util.lang.launchInUI
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach

abstract class ViewerConfig {

    var imagePropertyChangedListener: (() -> Unit)? = null

    fun <T> Preference<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {}
    ) {
        asFlow()
            .onEach { valueAssignment(it) }
            .drop(1)
            .distinctUntilChanged()
            .onEach { onChanged(it) }
            .launchInUI()
    }
}
