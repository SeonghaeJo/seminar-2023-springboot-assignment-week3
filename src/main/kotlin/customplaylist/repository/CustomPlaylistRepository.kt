package com.wafflestudio.seminar.spring2023.customplaylist.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CustomPlaylistRepository : JpaRepository<CustomPlaylistEntity, Long> {

    @Query("""
        SELECT cp FROM custom_playlists cp
        LEFT JOIN FETCH cp.songs cps
        LEFT JOIN FETCH cps.song s
        WHERE cp.id = :id AND cp.userId = :userId
    """)
    fun findByIdAndUserIdJoinFetchSongs(id: Long, userId: Long): CustomPlaylistEntity?

    fun findByIdAndUserId(id: Long, userId: Long): CustomPlaylistEntity?

    fun findAllByUserId(userId: Long): List<CustomPlaylistEntity>
}
