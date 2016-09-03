package eu.kanade.tachiyomi.widget

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener

class IgnoreFirstSpinnerListener(private val block: (AdapterView<*>?, Int) -> Unit) : OnItemSelectedListener {

    private var firstEvent = true

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!firstEvent) {
            block(parent, position)
        } else {
            firstEvent = false
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}