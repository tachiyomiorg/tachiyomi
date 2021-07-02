package eu.kanade.tachiyomi.widget.listener

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener

class IgnoreFirstSpinnerListener(private val block: (Int) -> Unit) : OnItemSelectedListener {

    private var firstEvent = true

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!firstEvent) {
            block(position)
        } else {
            firstEvent = false
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}
