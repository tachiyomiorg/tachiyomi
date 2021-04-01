package eu.kanade.tachiyomi.ui.reader.setting

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.annotation.ArrayRes
import androidx.appcompat.widget.PopupMenu
import com.tfcporciuncula.flow.Preference
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.SpinnerPreferenceBinding

class SpinnerPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    FrameLayout(context, attrs) {

    private var entries = emptyList<String>()
    private var popup: PopupMenu? = null

    var onItemSelectedListener: ((Int) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                popup = makeSettingsPopup()
                setOnTouchListener(popup?.dragToOpenListener)
                setOnClickListener {
                    popup?.show()
                }
            }
        }

    private val binding = SpinnerPreferenceBinding.inflate(LayoutInflater.from(context), this, false)

    init {
        addView(binding.root)

        val attr = context.obtainStyledAttributes(attrs, R.styleable.SpinnerPreference)

        val title = attr.getString(R.styleable.SpinnerPreference_title).orEmpty()
        binding.title.text = title

        val entries = (attr.getTextArray(R.styleable.SpinnerPreference_android_entries) ?: emptyArray()).map { it.toString() }
        this.entries = entries
        binding.details.text = entries.firstOrNull().orEmpty()

        attr.recycle()
    }

    fun setSelection(selection: Int) {
        binding.details.text = entries.getOrNull(selection).orEmpty()
    }

    fun bindToPreference(pref: Preference<Int>, offset: Int = 0, block: ((Int) -> Unit)? = null) {
        setSelection(pref.get() - offset)

        popup = makeSettingsPopup(pref, offset, block)
        setOnTouchListener(popup?.dragToOpenListener)
        setOnClickListener {
            popup?.show()
        }
    }

    inline fun <reified T : Enum<T>> bindToPreference(pref: Preference<T>) {
        val enumConstants = T::class.java.enumConstants
        enumConstants?.indexOf(pref.get())?.let { setSelection(it) }

        val popup = makeSettingsPopup(pref)
        setOnTouchListener(popup.dragToOpenListener)
        setOnClickListener {
            popup.show()
        }
    }

    fun bindToIntPreference(pref: Preference<Int>, @ArrayRes intValuesResource: Int, block: ((Int) -> Unit)? = null) {
        val intValues = resources.getStringArray(intValuesResource).map { it.toIntOrNull() }
        setSelection(intValues.indexOf(pref.get()))

        popup = makeSettingsPopup(pref, intValues, block)
        setOnTouchListener(popup?.dragToOpenListener)
        setOnClickListener {
            popup?.show()
        }
    }

    inline fun <reified T : Enum<T>> makeSettingsPopup(preference: Preference<T>): PopupMenu {
        return createPopupMenu { pos ->
            onItemSelectedListener?.invoke(pos)

            val enumConstants = T::class.java.enumConstants
            enumConstants?.get(pos)?.let { enumValue -> preference.set(enumValue) }
        }
    }

    private fun makeSettingsPopup(preference: Preference<Int>, intValues: List<Int?>, block: ((Int) -> Unit)? = null): PopupMenu {
        return createPopupMenu { pos ->
            preference.set(intValues[pos] ?: 0)
            block?.invoke(pos)
        }
    }

    private fun makeSettingsPopup(preference: Preference<Int>, offset: Int = 0, block: ((Int) -> Unit)? = null): PopupMenu {
        return createPopupMenu { pos ->
            preference.set(pos + offset)
            block?.invoke(pos)
        }
    }

    private fun makeSettingsPopup(): PopupMenu {
        return createPopupMenu { pos ->
            onItemSelectedListener?.invoke(pos)
        }
    }

    private fun menuClicked(menuItem: MenuItem): Int {
        val pos = menuItem.itemId
        setSelection(pos)
        return pos
    }

    fun createPopupMenu(onItemClick: (Int) -> Unit): PopupMenu {
        val popup = PopupMenu(context, this, Gravity.END, R.attr.actionOverflowMenuStyle, 0)
        entries.forEachIndexed { index, entry ->
            popup.menu.add(0, index, 0, entry)
        }
        popup.setOnMenuItemClickListener { menuItem ->
            val pos = menuClicked(menuItem)
            onItemClick(pos)
            true
        }
        return popup
    }
}
