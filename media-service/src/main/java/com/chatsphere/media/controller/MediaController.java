package com.chatsphere.media.controller;

import com.chatsphere.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<MediaService.UploadResult> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(mediaService.uploadFile(file));
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<byte[]> getFile(@PathVariable String filename) {
        byte[] data = mediaService.loadLocalFile(filename);
        HttpHeaders headers = new HttpHeaders();

        // Resolve Content Type
        if (filename.endsWith(".png")) headers.setContentType(MediaType.IMAGE_PNG);
        else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) headers.setContentType(MediaType.IMAGE_JPEG);
        else if (filename.endsWith(".gif")) headers.setContentType(MediaType.IMAGE_GIF);
        else if (filename.endsWith(".webp")) headers.setContentType(MediaType.parseMediaType("image/webp"));
        else if (filename.endsWith(".pdf")) headers.setContentType(MediaType.APPLICATION_PDF);
        else if (filename.endsWith(".webm")) headers.setContentType(MediaType.parseMediaType("audio/webm"));
        else if (filename.endsWith(".mp3")) headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        else headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
