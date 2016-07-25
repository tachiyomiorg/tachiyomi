package eu.kanade.tachiyomi.util

import android.widget.Spinner

fun Spinner.setPositionByValue(value: String) {
    var index = 0

    for (i in 0..this.count - 1) {
        if (this.getItemAtPosition(i).toString().equals(value, true)) {
            index = i
            break
        }
    }
    this.setSelection(index)
}