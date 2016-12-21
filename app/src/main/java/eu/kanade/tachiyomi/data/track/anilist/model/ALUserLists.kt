package eu.kanade.tachiyomi.data.track.anilist.model

data class ALUserLists(val lists: Map<String, List<ALUserManga>>) {

    fun flatten() = lists.values.flatten()
}