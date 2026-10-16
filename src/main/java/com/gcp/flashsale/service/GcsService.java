package com.gcp.flashsale.service;

import java.nio.charset.StandardCharsets;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Cloud Storage access (replaces S3Service).
 * Credentials come from the VM / Cloud Run service account, no key files.
 */
@Service
public class GcsService {

    private final String bucket;
    private volatile Storage storage; // created on first use

    public GcsService(@Value("${flashsale.gcs.bucket:}") String bucket) {
        this.bucket = bucket;
    }

    public void uploadText(String key, String content) {
        upload(key, "text/plain; charset=utf-8", content.getBytes(StandardCharsets.UTF_8));
    }

    public void uploadPdf(String key, byte[] content) {
        upload(key, "application/pdf", content);
    }

    public String downloadText(String key) {
        byte[] bytes = storage().readAllBytes(blobId(key));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void upload(String key, String contentType, byte[] content) {
        BlobInfo info = BlobInfo.newBuilder(blobId(key)).setContentType(contentType).build();
        storage().create(info, content);
    }

    private BlobId blobId(String key) {
        if (bucket.isBlank()) {
            throw new IllegalStateException("flashsale.gcs.bucket (env GCS_BUCKET) is not set");
        }
        return BlobId.of(bucket, key);
    }

    private Storage storage() {
        if (storage == null) {
            synchronized (this) {
                if (storage == null) {
                    storage = StorageOptions.getDefaultInstance().getService();
                }
            }
        }
        return storage;
    }
}