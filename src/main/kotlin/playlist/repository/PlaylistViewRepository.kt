package com.wafflestudio.seminar.spring2023.playlist.repository

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface PlaylistViewRepository : JpaRepository<PlaylistViewEntity, Long> {

    // Pessimistic Exclusive lock
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllByPlaylistIdAndUserId(playlistId: Long, viewId: Long) : List<PlaylistViewEntity>

    fun findAllByPlaylistId(playlistId: Long) : List<PlaylistViewEntity>
}
