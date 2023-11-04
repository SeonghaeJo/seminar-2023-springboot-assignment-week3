package com.wafflestudio.seminar.spring2023.admin.service

import com.wafflestudio.seminar.spring2023.song.repository.*
import org.hibernate.exception.ConstraintViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class AdminBatchServiceImpl (
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val songRepository: SongRepository,
    private val txManager: PlatformTransactionManager
        ) : AdminBatchService {

    private val txTemplate = TransactionTemplate(txManager)
    private val threadPool = Executors.newFixedThreadPool(4)

    override fun insertAlbums(albumInfos: List<BatchAlbumInfo>) {
        val futures = mutableListOf<Future<Boolean>>()
        for (albumInfo in albumInfos) {
            futures.add(
                threadPool.submit<Boolean> {
                    txTemplate.execute {
                        insertAlbumInfo(albumInfo)
                    }
                }
            )
        }
        // Wait for all tasks
        for (future in futures) {
            future.get()
        }
    }

    private fun insertAlbumInfo(albumInfo: BatchAlbumInfo): Boolean {
        val artistEntity = getArtistEntityByName(albumInfo.artist)
        val albumEntity = albumRepository.save(
            AlbumEntity(
                title = albumInfo.title,
                image = albumInfo.image,
                artist = artistEntity
            )
        )
        for (songInfo in albumInfo.songs) {
            val songEntity = songRepository.save(
                SongEntity(
                    title = songInfo.title,
                    duration = songInfo.duration,
                    album = albumEntity,
                )
            )
            for (artistName in songInfo.artists) {
                songEntity.artists.add(SongArtistEntity(
                    song = songEntity, artist = getArtistEntityByName(artistName)
                )) // JPA Change detection
            }
        }
        return true
    }

    private fun getArtistEntityByName(name : String): ArtistEntity {
        var artistEntity = artistRepository.findByName(name)
        if (artistEntity == null) {
            // artist name is unique key
            artistEntity = ArtistEntity(name = name)
            try {
                artistRepository.save(artistEntity)
            } catch (_: ConstraintViolationException) {
                // Exception handling for inserting duplicated value into unique key
                artistEntity = artistRepository.findByName(name)
            }
        }
        return artistEntity
    }
}
