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

import java.lang.Exception

@Suppress("unused")
class RatelimitException : Exception {

  constructor() : super()

  constructor(message: String) : super(message)

  constructor(cause: Exception) : super(cause)

  constructor(message: String, cause: Exception) : super(message, cause)
}