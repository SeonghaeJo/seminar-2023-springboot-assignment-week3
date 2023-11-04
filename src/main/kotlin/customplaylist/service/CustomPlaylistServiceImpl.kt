package com.wafflestudio.seminar.spring2023.customplaylist.service

import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistEntity
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistRepository
import com.wafflestudio.seminar.spring2023.customplaylist.repository.CustomPlaylistSongEntity
import com.wafflestudio.seminar.spring2023.song.repository.SongRepository
import com.wafflestudio.seminar.spring2023.song.service.Song
import jakarta.persistence.OptimisticLockException
import jakarta.transaction.Transactional
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * 스펙:
 *  1. 커스텀 플레이리스트 생성시, 자동으로 생성되는 제목은 "내 플레이리스트 #{내 커스텀 플레이리스트 갯수 + 1}"이다.
 *  2. 곡 추가 시  CustomPlaylistSongEntity row 생성, CustomPlaylistEntity의 songCnt의 업데이트가 atomic하게 동작해야 한다. (둘 다 모두 성공하거나, 둘 다 모두 실패해야 함)
 *
 * 조건:
 *  1. Synchronized 사용 금지.
 *  2. 곡 추가 요청이 동시에 들어와도 동시성 이슈가 없어야 한다.(PlaylistViewServiceImpl에서 동시성 이슈를 해결한 방법과는 다른 방법을 사용할 것)
 *  3. JPA의 변경 감지 기능을 사용해야 한다.
 */
@Service
class CustomPlaylistServiceImpl (
    private val customPlaylistRepository: CustomPlaylistRepository,
    private val songRepository: SongRepository,
    private val txManager: PlatformTransactionManager
        ) : CustomPlaylistService {

    private val txTemplate = TransactionTemplate(txManager)

    override fun get(userId: Long, customPlaylistId: Long): CustomPlaylist {
        val customPlaylistEntity = customPlaylistRepository.findByIdAndUserIdJoinFetchSongs(
                id = customPlaylistId, userId = userId
        ) ?: throw CustomPlaylistNotFoundException()
        val songEntities = songRepository.findAllByIdWithJoinFetch(
            customPlaylistEntity.songs.map {it.song.id}.toList()
        )
        return CustomPlaylist(
            id = customPlaylistEntity.id,
            title = customPlaylistEntity.title,
            songs = songEntities.map { Song(it) }.toList()
        )
    }

    override fun gets(userId: Long): List<CustomPlaylistBrief> {
        return customPlaylistRepository.findAllByUserId(userId).map {
                CustomPlaylistBrief(id = it.id, title = it.title, songCnt = it.songCnt)
        }.toList()
    }

    override fun create(userId: Long): CustomPlaylistBrief {
        val userPlaylistCnt = gets(userId).size
        val title = "내 플레이리스트 #${userPlaylistCnt + 1}"
        val customPlaylistEntity = customPlaylistRepository.save(
                CustomPlaylistEntity(userId = userId, title = title)
        )
        return CustomPlaylistBrief(
            id = customPlaylistEntity.id,
            title = customPlaylistEntity.title,
            songCnt = customPlaylistEntity.songCnt
        )
    }

    override fun patch(userId: Long, customPlaylistId: Long, title: String): CustomPlaylistBrief {
        return txTemplate.execute {
            val customPlaylistEntity = customPlaylistRepository.findByIdAndUserId(
                id = customPlaylistId, userId = userId
            ) ?: throw CustomPlaylistNotFoundException()
            customPlaylistEntity.title = title // JPA change detection
            CustomPlaylistBrief(
                id = customPlaylistId,
                title = title,
                songCnt = customPlaylistEntity.songCnt,
            )
        } ?: throw CustomPlaylistNotFoundException()
    }

    override fun addSong(userId: Long, customPlaylistId: Long, songId: Long): CustomPlaylistBrief {
        return addSongWithOptimisticLock(userId, customPlaylistId, songId, 0)
    }

    private val maxAttemptCnt = 10

    private fun addSongWithOptimisticLock(
        userId: Long, customPlaylistId: Long, songId: Long, attemptCnt: Int
    ): CustomPlaylistBrief {
        if (attemptCnt > maxAttemptCnt) {
            throw OptimisticLockingFailureException("Reached maximum attempts")
        }
        try {
            return txTemplate.execute {
                val customPlaylistEntity = customPlaylistRepository.findByIdAndUserIdJoinFetchSongs(
                    id = customPlaylistId, userId = userId
                ) ?: throw CustomPlaylistNotFoundException()
                val songEntities = songRepository.findAllByIdWithJoinFetch(listOf(songId))
                if (songEntities.isEmpty()) {
                    throw SongNotFoundException()
                }
                customPlaylistEntity.songs.add(
                    CustomPlaylistSongEntity(
                        customPlaylist = customPlaylistEntity,
                        song = songEntities[0]
                    )
                ) // JPA change detection (CustomPlaylistSongEntity row automatically inserted)
                customPlaylistEntity.songCnt++ // JPA change detection
                CustomPlaylistBrief(
                    id = customPlaylistId,
                    title = customPlaylistEntity.title,
                    songCnt = customPlaylistEntity.songCnt
                )
            } ?: throw CustomPlaylistNotFoundException()
        } catch (_: OptimisticLockingFailureException) {
            // Terminate old tx and Retry new tx if old tx under optimistic lock fails
            Thread.sleep(100L)
            return addSongWithOptimisticLock(userId, customPlaylistId, songId, attemptCnt + 1)
        }
    }
}
