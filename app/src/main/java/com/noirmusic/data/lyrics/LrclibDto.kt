package com.noirmusic.data.lyrics

import kotlinx.serialization.json.Json

/** JSON configuration shared by the LRCLIB client and tests. */
val LrclibJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}
