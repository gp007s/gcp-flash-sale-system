package com.gcp.flashsale.controller;

import com.gcp.flashsale.service.PubSubService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Connectivity test for the topic (same idea as /sqs/send in the AWS project). Needs X-Admin-Key. */
@RestController
@RequestMapping("/pubsub")
public class PubSubController {

    private final PubSubService pubSubService;

    public PubSubController(PubSubService pubSubService) {
        this.pubSubService = pubSubService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> send(@RequestBody String message) {
        try {
            String id = pubSubService.sendMessage(message);
            return ResponseEntity.ok("Message sent to Pub/Sub. ID: " + id);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Could not send the message to Pub/Sub");
        }
    }
}