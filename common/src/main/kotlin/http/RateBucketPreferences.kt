/*
 *
 * Copyright (C) 2018 The Tachiyomi Open Source Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package tachiyomi.core.http

import tachiyomi.core.prefs.Preference
import tachiyomi.core.prefs.PreferenceStore

class RateBucketPreferences(private val preferenceStore: PreferenceStore) {
  fun getBucket(sourceName: String, capacity: Int, rate: Long): Preference<RateBucket> {
    return preferenceStore.getObject(
      key = sourceName,
      defaultValue = RateBucket(capacity, rate),
      serializer = { it.serialize() },
      deserializer = { RateBucket.deserialize(it) }
    )
  }

}