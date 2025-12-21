package com.priceprocessor.services;

import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.interfaces.PriceClient;
import com.priceprocessor.services.queue.NotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriceUpdateService {

    private final ProductRepository productRepository;
    private final PriceClient priceClient;
    private final NotificationProducer notificationProducer;

    private static final int BATCH_SIZE = 5;

    @Transactional
    public int updateOutdatedPrices() {
        List<ProductObservation> productsToUpdate = productRepository.findProductsToUpdate(PageRequest.of(0, BATCH_SIZE));

        if (productsToUpdate.isEmpty()) {
            log.info("No products to update.");
            return 0;
        }

        log.info("Starting batch update for {} products", productsToUpdate.size());

        for (ProductObservation product : productsToUpdate) {
            updateSingleProduct(product);
        }

        return productsToUpdate.size();
    }

    private void updateSingleProduct(ProductObservation product) {
        try {
            log.info("Checking price for: {}", product.getProductName());

            Optional<PriceResponse> response = priceClient.checkPriceByUrl(product.getProductUrl());

            if (response.isPresent()) {
                BigDecimal newPrice = response.get().price();
                BigDecimal oldPrice = product.getCurrentPrice();

                product.addPriceHistory(newPrice, LocalDateTime.now());
                product.setProductName(response.get().foundProductName());
                if (newPrice.compareTo(oldPrice) < 0) {
                    notificationProducer.sendEmailNotification(
                            product.getUserEmail(),
                            "Price Drop Alert!",
                            "Price for " + product.getProductName() + " dropped from " + oldPrice + " to " + newPrice
                    );
                }
            } else {
                log.warn("Could not fetch price for {}", product.getProductUrl());
            }

        } catch (Exception e) {
            log.error("Error updating product id: {}", product.getId(), e);
        } finally {
            product.setLastCheckedAt(LocalDateTime.now());
            productRepository.save(product);
        }
    }
}