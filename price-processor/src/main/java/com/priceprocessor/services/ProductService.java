package com.priceprocessor.services;

import com.priceprocessor.dtos.api.*;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.exceptions.ProductNotFoundException;
import com.priceprocessor.exceptions.ProductNotFoundInStoreException;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.clients.PriceClient;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceClient priceClient;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional(readOnly = true)
    @Timed(value = "product.details.fetch", description = "Time taken to fetch product details")
    public ProductDetailsResponse getProductDetails(Long id) {
        String currentUser = getCurrentUserEmail();

        ProductObservation product = productRepository.findByIdAndUserEmail(id, currentUser)
                .orElseThrow(() -> new ProductNotFoundException(id));

        List<ProductDetailsResponse.PriceHistoryDto> historyDtos = product.getPriceHistory().stream()
                .map(h -> new ProductDetailsResponse.PriceHistoryDto(h.getPrice(), h.getCheckedAt()))
                .toList();

        return new ProductDetailsResponse(
                product.getId(),
                product.getProductName(),
                product.getProductUrl(),
                product.getCurrentPrice(),
                product.getUserEmail(),
                historyDtos
        );
    }

    @Transactional
    public ProductObservationResponse startObservingProductByName(ProductObservationByNameRequest request) {
        log.info("User {} requested observation for product name: {}", getCurrentUserEmail(), request.productName());

        Optional<PriceResponse> response = priceClient.checkPriceByName(request.productName());

        if (response.isEmpty()) {
            throw new ProductNotFoundInStoreException(request.productName());
        }

        return saveNewProductObservation(response.get());
    }

    @Transactional
    public ProductObservationResponse startObservingProductByUrl(ProductObservationByUrlRequest request) {
        log.info("User {} requested observation for URL: {}", getCurrentUserEmail(), request.productUrl());

        Optional<PriceResponse> response = priceClient.checkPriceByUrl(request.productUrl());

        if (response.isEmpty()) {
            throw new ProductNotFoundInStoreException(request.productUrl());
        }

        return saveNewProductObservation(response.get());
    }

    private ProductObservationResponse saveNewProductObservation(PriceResponse priceResponse) {
        String currentUser = getCurrentUserEmail();

        ProductObservation observation = ProductObservation.builder()
                .productName(priceResponse.foundProductName())
                .productUrl(priceResponse.ceneoUrl())
                .userEmail(currentUser)
                .build();

        observation.addPriceHistory(priceResponse.price(), LocalDateTime.now());

        ProductObservation saved = productRepository.save(observation);
        log.info("Started observing product ID: {} for user: {}", saved.getId(), currentUser);

        return ProductObservationResponse.mapToDto(saved);
    }

    public List<ProductObservationResponse> getAllObservedProducts() {
        String currentUser = getCurrentUserEmail();

        return productRepository.findAllByUserEmail(currentUser).stream()
                .map(ProductObservationResponse::mapToDto)
                .toList();
    }

    @Transactional
    public void deleteObservedProduct(Long id) {
        String currentUser = getCurrentUserEmail();

        ProductObservation product = productRepository.findByIdAndUserEmail(id, currentUser)
                .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.delete(product);
        log.info("Deleted product ID: {} for user: {}", id, currentUser);
    }
}