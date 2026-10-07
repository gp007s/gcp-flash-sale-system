package com.gcp.flashsale.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.gcp.flashsale.model.OrderFulfillmentDTO;
import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Publishes a fulfilled-order notification to the order-notifications Pub/Sub topic. */
@Service
@ConditionalOnProperty(name = "flashsale.role", havingValue = "fulfilment")
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final String project;
    private final String notificationsTopic;
    private Publisher publisher;

    public NotificationService(
            @Value("${flashsale.pubsub.project}") String project,
            @Value("${NOTIFICATIONS_TOPIC:order-notifications}") String notificationsTopic) {
        this.project = project;
        this.notificationsTopic = notificationsTopic;
    }

    public void sendOrderNotification(OrderFulfillmentDTO order) throws Exception {
        String body = String.format(
                "Order %s fulfilled for %s — %s x%d — $%.2f",
                order.getOrderId(),
                order.getUserName(),
                order.getItemDetails(),
                order.getQuantity(),
                order.getPaymentAmount());
        PubsubMessage message = PubsubMessage.newBuilder()
                .setData(ByteString.copyFromUtf8(body))
                .build();
        ApiFuture<String> future = publisher().publish(message);
        String messageId = future.get(5, TimeUnit.SECONDS);
        log.info("Notification published to {}, messageId={}", notificationsTopic, messageId);
    }

    private synchronized Publisher publisher() throws IOException {
        if (publisher == null) {
            publisher = Publisher.newBuilder(TopicName.of(project, notificationsTopic)).build();
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
