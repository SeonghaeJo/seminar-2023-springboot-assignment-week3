package com.wafflestudio.seminar.spring2023.admin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.wafflestudio.seminar.spring2023.admin.service.AdminBatchService
import com.wafflestudio.seminar.spring2023.admin.service.BatchAlbumInfo
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.concurrent.Executors

@RestController
class AdminController (
    private val adminBatchService: AdminBatchService,
    private val objectMapper: ObjectMapper
        ) {

    @PostMapping("/admin/v1/batch/albums")
    fun insertAlbums(
        @RequestPart("albums.txt") file: MultipartFile,
    ) {
        val batchAlbumInfos : List<BatchAlbumInfo> = objectMapper.readValue(file.inputStream.reader().readText())
        adminBatchService.insertAlbums(batchAlbumInfos)
    }
}
