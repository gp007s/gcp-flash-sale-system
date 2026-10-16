package com.gcp.flashsale.model;

import java.util.Map;

/**
 * The JSON that a Pub/Sub push subscription sends to our endpoint.
 * "data" is the order message, base64 encoded.
 */
public record PubSubPushRequest(Message message, String subscription) {

    public record Message(String data, String messageId, Map<String, String> attributes) {
    }
}