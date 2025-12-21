package com.priceprocessor.scheduler;

import com.priceprocessor.services.PriceUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class LocalDevScheduler {

    private final PriceUpdateService priceUpdateService;

    @Scheduled(fixedDelay = 60000)
    public void runLocalBatch() {
        log.info("[LOCAL DEV] Triggering scheduled price update...");
        priceUpdateService.updateOutdatedPrices();
    }
}