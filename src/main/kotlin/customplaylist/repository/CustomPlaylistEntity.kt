package com.wafflestudio.seminar.spring2023.customplaylist.repository

import jakarta.persistence.*

@Entity(name = "custom_playlists")
class CustomPlaylistEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    val userId: Long,
    var title: String,
    @OneToMany(mappedBy = "customPlaylist", cascade = [CascadeType.ALL], orphanRemoval = true)
    val songs: MutableList<CustomPlaylistSongEntity> = mutableListOf(),
    @Version // Versioning for Optimistic Shared Lock
    var songCnt: Int = 0,
)
