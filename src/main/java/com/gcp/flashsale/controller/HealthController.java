package com.gcp.flashsale.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Shows which VM answered. Handy to see the load balancer spreading traffic. Use it as the LB health check too. */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<String> checkHealth() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            return ResponseEntity.ok("GCP FlashSaleSystem is running on the Host/IP : "
                    + localHost.getHostName() + "/" + localHost.getHostAddress());
        } catch (UnknownHostException e) {
            return ResponseEntity.ok("FlashSaleSystem is healthy");
        }
    }
}