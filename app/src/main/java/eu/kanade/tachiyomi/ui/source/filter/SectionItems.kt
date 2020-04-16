package eu.kanade.tachiyomi.ui.source.filter

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
        if (javaClass != other?.javaClass) return false
        return filter == (other as TriStateSectionItem).filter
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
        if (javaClass != other?.javaClass) return false
        return filter == (other as TextSectionItem).filter
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
        if (javaClass != other?.javaClass) return false
        return filter == (other as CheckboxSectionItem).filter
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
        if (javaClass != other?.javaClass) return false
        return filter == (other as SelectSectionItem).filter
    }

    override fun hashCode(): Int {
        return filter.hashCode()
    }
}
