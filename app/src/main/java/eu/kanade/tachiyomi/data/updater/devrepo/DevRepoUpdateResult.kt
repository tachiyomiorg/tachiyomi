package eu.kanade.tachiyomi.data.updater.devrepo

import eu.kanade.tachiyomi.data.updater.UpdateResult

sealed class DevRepoUpdateResult : UpdateResult() {

    class NewUpdate(release: DevRepoRelease): UpdateResult.NewUpdate<DevRepoRelease>(release)
    class NoNewUpdate: UpdateResult.NoNewUpdate()

}
