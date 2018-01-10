package eu.kanade.tachiyomi.ui.setting

import android.support.graphics.drawable.VectorDrawableCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.preference.*
import eu.kanade.tachiyomi.widget.preference.IntListPreference

@DslMarker
@Target(AnnotationTarget.TYPE)
annotation class DSL

inline fun PreferenceManager.newScreen(block: (@DSL PreferenceScreen).() -> Unit): PreferenceScreen {
    return createPreferenceScreen(context).also { it.block() }
}

inline fun PreferenceGroup.preference(block: (@DSL Preference).() -> Unit): Preference {
    return initThenAdd(Preference(context), block)
}

inline fun PreferenceGroup.switchPreference(block: (@DSL SwitchPreferenceCompat).() -> Unit): SwitchPreferenceCompat {
    return initThenAdd(SwitchPreferenceCompat(context), block)
}

inline fun PreferenceGroup.checkBoxPreference(block: (@DSL CheckBoxPreference).() -> Unit): CheckBoxPreference {
    return initThenAdd(CheckBoxPreference(context), block)
}

inline fun PreferenceGroup.editTextPreference(block: (@DSL EditTextPreference).() -> Unit): EditTextPreference {
    return initThenAdd(EditTextPreference(context), block).also(::initDialog)
}

inline fun PreferenceGroup.listPreference(block: (@DSL ListPreference).() -> Unit): ListPreference {
    return initThenAdd(ListPreference(context), block).also(::initDialog)
}

inline fun PreferenceGroup.intListPreference(block: (@DSL IntListPreference).() -> Unit): IntListPreference {
    return initThenAdd(IntListPreference(context), block).also(::initDialog)
}

inline fun PreferenceGroup.multiSelectListPreference(block: (@DSL MultiSelectListPreference).() -> Unit): MultiSelectListPreference {
    return initThenAdd(MultiSelectListPreference(context), block).also(::initDialog)
}

inline fun PreferenceScreen.preferenceCategory(block: (@DSL PreferenceCategory).() -> Unit): PreferenceCategory {
    return addThenInit(PreferenceCategory(context), block)
}

inline fun PreferenceScreen.preferenceScreen(block: (@DSL PreferenceScreen).() -> Unit): PreferenceScreen {
    return addThenInit(preferenceManager.createPreferenceScreen(context), block)
}

fun initDialog(dialogPreference: DialogPreference) {
    with(dialogPreference) {
        if (dialogTitle == null) {
            dialogTitle = title
        }
    }
}

inline fun <P : Preference> PreferenceGroup.initThenAdd(p: P, block: P.() -> Unit): P {
    return p.apply { block(); addPreference(this); }
}

inline fun <P : Preference> PreferenceGroup.addThenInit(p: P, block: P.() -> Unit): P {
    return p.apply { addPreference(this); block() }
}

inline fun Preference.onClick(crossinline block: () -> Unit) {
    setOnPreferenceClickListener { block(); true }
}

inline fun Preference.onChange(crossinline block: (Any?) -> Boolean) {
    setOnPreferenceChangeListener { _, newValue -> block(newValue) }
}

var Preference.defaultValue: Any?
    get() = null // set only
    set(value) { setDefaultValue(value) }

var Preference.titleRes: Int
    get() = 0 // set only
    set(value) { setTitle(value) }

var Preference.iconRes: Int
    get() = 0 // set only
    set(value) { icon = VectorDrawableCompat.create(context.resources, value, context.theme) }

var Preference.summaryRes: Int
    get() = 0 // set only
    set(value) { setSummary(value) }

var Preference.iconTint: Int
    get() = 0 // set only
    set(value) { DrawableCompat.setTint(icon, value) }

var ListPreference.entriesRes: Array<Int>
    get() = emptyArray() // set only
    set(value) { entries = value.map { context.getString(it) }.toTypedArray() }

var MultiSelectListPreference.entriesRes: Array<Int>
    get() = emptyArray() // set only
    set(value) { entries = value.map { context.getString(it) }.toTypedArray() }
