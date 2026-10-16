package com.gcp.flashsale.controller;

import com.gcp.flashsale.service.GcsService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Connectivity test for the bucket (same endpoints as /s3/* in the AWS project). Needs X-Admin-Key. */
@RestController
@RequestMapping("/gcs")
public class GcsController {

    private final GcsService gcsService;

    public GcsController(GcsService gcsService) {
        this.gcsService = gcsService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam String key, @RequestBody String content) {
        gcsService.uploadText(key, content);
        return ResponseEntity.ok("Uploaded the form-data to Cloud Storage with key: " + key);
    }

    @PostMapping("/upload/pdf")
    public ResponseEntity<String> uploadPdf(@RequestParam String key, @RequestBody byte[] content) {
        gcsService.uploadPdf(key, content);
        return ResponseEntity.ok("Uploaded the PDF to Cloud Storage with key: " + key);
    }

    @GetMapping("/download")
    public ResponseEntity<String> download(@RequestParam String key) {
        return ResponseEntity.ok(gcsService.downloadText(key));
    }
}