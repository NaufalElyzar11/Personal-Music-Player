package com.noirmusic.data.playlist

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Playlist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trackIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)
