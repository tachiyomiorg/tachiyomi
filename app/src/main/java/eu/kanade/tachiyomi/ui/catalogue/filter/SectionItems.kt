package eu.kanade.tachiyomi.ui.catalogue.filter

import eu.davidea.flexibleadapter.items.ISectionable
import eu.kanade.tachiyomi.source.model.Filter

class TriStateSectionItem(filter: Filter.TriState) : TriStateItem(filter), ISectionable<TriStateItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is TriStateSectionItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }
}

class TextSectionItem(filter: Filter.Text) : TextItem(filter), ISectionable<TextItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is TextSectionItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }
}

class CheckboxSectionItem(filter: Filter.CheckBox) : CheckboxItem(filter), ISectionable<CheckboxItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is CheckboxSectionItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }
}

class SelectSectionItem(filter: Filter.Select<*>) : SelectItem(filter), ISectionable<SelectItem.Holder, GroupItem> {

    private var head: GroupItem? = null

    override fun getHeader(): GroupItem? = head

    override fun setHeader(header: GroupItem?) {
        head = header
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is SelectSectionItem) {
            return filter == other.filter
        }
        return false
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }
}
