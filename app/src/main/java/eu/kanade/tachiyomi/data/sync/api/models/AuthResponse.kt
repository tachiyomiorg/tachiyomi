package eu.kanade.tachiyomi.data.sync.api.models

/**
 * Auth response
 */

data class AuthResponse(val success: Boolean,
                        val error: String,
                        val token: String)