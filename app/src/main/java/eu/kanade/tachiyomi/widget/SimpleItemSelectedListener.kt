package eu.kanade.tachiyomi.widget

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener

class SimpleItemSelectedListener(private val callback: (Int) -> Unit): OnItemSelectedListener {

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        callback(position)
    }
}