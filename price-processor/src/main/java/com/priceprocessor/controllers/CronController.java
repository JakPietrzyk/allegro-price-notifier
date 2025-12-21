package com.priceprocessor.controllers;

import com.priceprocessor.services.PriceUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cron")
@RequiredArgsConstructor
@Slf4j
public class CronController {

    private final PriceUpdateService priceUpdateService;

    @PostMapping("/update-prices")
    public ResponseEntity<String> triggerBatchUpdate() {
        log.info("Received cron request to update prices");
        int updatedCount = priceUpdateService.updateOutdatedPrices();
        return ResponseEntity.ok("Batch update finished. Processed: " + updatedCount);
    }
}