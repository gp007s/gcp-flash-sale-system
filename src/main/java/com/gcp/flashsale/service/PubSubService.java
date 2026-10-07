package com.gcp.flashsale.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends the order message to a Pub/Sub topic (replaces SqsService).
 * Waits for the message id, so we know Pub/Sub accepted it before we tell the user.
 */
@Service
public class PubSubService {

    private static final Logger log = LoggerFactory.getLogger(PubSubService.class);

    private final String project;
    private final String topic;
    private Publisher publisher; // created on first use, so the app starts without GCP access

    public PubSubService(
            @Value("${flashsale.pubsub.project}") String project,
            @Value("${flashsale.pubsub.topic}") String topic) {
        this.project = project;
        this.topic = topic;
    }

    /** Returns the Pub/Sub message id. Throws if the message was not accepted. */
    public String sendMessage(String body) throws Exception {
        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(body))
                .build();
        ApiFuture<String> future = publisher().publish(message);
        String messageId = future.get(5, TimeUnit.SECONDS);
        log.info("Order sent to Pub/Sub topic {}, messageId={}", topic, messageId);
        return messageId;
    }

    private synchronized Publisher publisher() throws IOException {
        if (publisher == null) {
            publisher = Publisher.newBuilder(TopicName.of(project, topic)).build();
        }
        return publisher;
    }

    @PreDestroy
    synchronized void shutdown() {
        if (publisher != null) {
            publisher.shutdown();
            try {
                publisher.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}