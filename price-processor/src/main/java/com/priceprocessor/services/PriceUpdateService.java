package com.priceprocessor.services;

import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.exceptions.NotificationServiceException;
import com.priceprocessor.exceptions.PriceFetchException;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.clients.PriceClient;
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
    private final MetricsService metricsService;

    private static final int BATCH_SIZE = 5;

    @Transactional
    public int updateOutdatedPrices() {
        List<ProductObservation> productsToUpdate = productRepository.findProductsToUpdate(PageRequest.of(0, BATCH_SIZE));

        if (productsToUpdate.isEmpty()) {
            log.info("No products to update");
            return 0;
        }

        log.info("Starting batch update for {} products", productsToUpdate.size());

        for (ProductObservation product : productsToUpdate) {
            processProductUpdate(product);
        }

        return productsToUpdate.size();
    }

    private void processProductUpdate(ProductObservation product) {
        try {
            log.debug("Checking price for: {}", product.getProductName());

            Optional<PriceResponse> responseOpt = priceClient.checkPriceByUrl(product.getProductUrl());

            if (responseOpt.isPresent()) {
                metricsService.incrementProductPriceUpdateSuccess();
                updateProductData(product, responseOpt.get());
            } else {
                log.info("Product {} not found", product.getProductUrl());
                throw new PriceFetchException("Product not found");
            }

        } catch (PriceFetchException e) {
            metricsService.incrementLoginFailure(e.getClass().getSimpleName());
            log.error("Failed to update product ID: {}. Reason: {}", product.getId(), e.getMessage());
        } catch (Exception e) {
            metricsService.incrementLoginFailure(e.getClass().getSimpleName());
            log.error("Critical error updating product ID: {}", product.getId(), e);
        } finally {
            product.setLastCheckedAt(LocalDateTime.now());
            productRepository.save(product);
        }
    }

    private void updateProductData(ProductObservation product, PriceResponse response) {
        BigDecimal newPrice = response.price();
        BigDecimal oldPrice = product.getCurrentPrice();

        product.addPriceHistory(newPrice, LocalDateTime.now());
        product.setProductName(response.foundProductName());

        if (isPriceLower(newPrice, oldPrice)) {
            handlePriceDrop(product, oldPrice, newPrice);
        }
        log.info("Updated price for: {}", product.getProductName());
    }

    private boolean isPriceLower(BigDecimal newPrice, BigDecimal oldPrice) {
        return newPrice.compareTo(oldPrice) < 0;
    }

    private void handlePriceDrop(ProductObservation product, BigDecimal oldPrice, BigDecimal newPrice) {
        try {
            notificationProducer.sendEmailNotification(
                    product.getUserEmail(),
                    "Price Drop Alert!",
                    "Price for " + product.getProductName() + " dropped from " + oldPrice + " to " + newPrice
            );
        } catch (Exception e) {
            log.error("Price updated, but notification failed for user: {}", product.getUserEmail(), new NotificationServiceException("Email sending failed", e));
        }
    }
}