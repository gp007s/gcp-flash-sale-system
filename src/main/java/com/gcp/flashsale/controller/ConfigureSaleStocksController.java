package com.gcp.flashsale.controller;

import com.gcp.flashsale.service.RedisService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin side of the diagram: "Set Max StockCount". Needs the X-Admin-Key header. */
@RestController
@RequestMapping("/api/admin")
@ConditionalOnProperty(name = "flashsale.role", havingValue = "api", matchIfMissing = true)
public class ConfigureSaleStocksController {

    private static final Logger log = LoggerFactory.getLogger(ConfigureSaleStocksController.class);

    private final RedisService redisService;

    public ConfigureSaleStocksController(RedisService redisService) {
        this.redisService = redisService;
    }

    @PostMapping("/set-sale-stock")
    public ResponseEntity<String> configureMaximumCountStock(
            @RequestParam String itemNameInOffer,
            @RequestParam int maxItemQuantityOnSale) {
        if (itemNameInOffer.isBlank() || maxItemQuantityOnSale < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("itemNameInOffer is required and maxItemQuantityOnSale cannot be negative");
        }
        log.info("Setting stock to {} for the item in offer: {}", maxItemQuantityOnSale, itemNameInOffer);
        redisService.loadTheKeyValueFirstTime(RedisService.stockKey(itemNameInOffer), maxItemQuantityOnSale);
        return ResponseEntity.ok("Offer stock configured successfully for an item: "
                + itemNameInOffer + " = " + maxItemQuantityOnSale);
    }

    @GetMapping("/checkRemainingItemCount")
    public long getAvailableCount(@RequestParam String itemNameInOffer) {
        long remaining = redisService.getAvailableItemCount(itemNameInOffer);
        log.info("Remaining items for {}: {}", itemNameInOffer, remaining);
        return remaining;
    }
}