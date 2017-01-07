package eu.kanade.tachiyomi.data.source.model

class FilterList(list: List<Filter<*>>) : List<Filter<*>> by list {

    constructor(vararg fs: Filter<*>) : this(if (fs.isNotEmpty()) fs.asList() else emptyList())

    fun hasSameState(other: FilterList): Boolean {
        if (size != other.size) return false

        return (0..lastIndex)
                .all { get(it).javaClass == other[it].javaClass && get(it).state == other[it].state }
    }

}