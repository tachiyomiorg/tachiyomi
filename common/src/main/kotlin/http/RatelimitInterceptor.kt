/*
 *
 *  * Copyright (C) 2018 The Tachiyomi Open Source Project
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */

package tachiyomi.core.http

import okhttp3.Interceptor
import okhttp3.Response
import prefs.RateBucketPreferences
import tachiyomi.core.di.AppScope

class RatelimitInterceptor(
  val id: String,
  private val capacity: Int,
  private val refillRate: Long
) : Interceptor {

  @Synchronized
  override fun intercept(chain: Interceptor.Chain): Response {
    val preferences = AppScope.getInstance<RateBucketPreferences>()
    val prefBucket = preferences.getBucket(id, capacity, refillRate)
    val rateBucket = prefBucket.get()
    if (rateBucket.tryConsume()) {
      prefBucket.set(rateBucket)
      return chain.call().execute()
    } else {
      prefBucket.set(rateBucket)
      chain.call().cancel()
      throw RatelimitException("Currently ratelimited for $id!")
    }
  }

}