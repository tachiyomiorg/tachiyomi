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

class RateBucket(
  internal var capacity: Int,
  internal var refillRate: Long,
  internal var tokens: Int = capacity,
  internal var refillTime: Long = System.currentTimeMillis()
) {

  fun tryConsume(): Boolean {
    refill()
    return if (this.tokens >= 1) {
      this.tokens -= 1
      true
    } else {
      false
    }
  }

  fun tryConsume(tokens: Int): Boolean {
    refill()
    return if (this.tokens >= tokens && tokens <= this.capacity) {
      this.tokens -= tokens
      true
    } else {
      false
    }
  }

  fun getTokens(): Int {
    return this.tokens
  }

  private fun refill() {
    val now = System.currentTimeMillis()
    val toRefill = ((now - refillTime) / refillRate).toInt()
    this.tokens += toRefill
    if (this.tokens > this.capacity) {
      this.tokens = this.capacity
      refillTime = now
    }
    refillTime += refillRate
  }

  companion object
}

fun RateBucket.serialize(): String {
  return "$capacity;$refillRate;$tokens;$refillTime"
}

fun RateBucket.Companion.deserialize(serialized: String): RateBucket {
  val deserialized = serialized.split(";")
  return RateBucket(deserialized[0].toInt(), deserialized[1].toLong(),
    deserialized[2].toInt(),
    deserialized[3].toLong())
}