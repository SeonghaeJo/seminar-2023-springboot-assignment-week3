package com.wafflestudio.seminar.spring2023.playlist.repository

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity(name = "playlist_views")
class PlaylistViewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    val playlistId: Long,
    val userId: Long,

    val createdAt: LocalDateTime,
)
