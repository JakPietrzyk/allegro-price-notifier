package com.priceprocessor.services;

import com.priceprocessor.dtos.api.*;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.clients.PriceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceClient priceClient;

    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long id) {
        String currentUser = getCurrentUserEmail();

        ProductObservation product = productRepository.findByIdAndUserEmail(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Product not found or access denied"));

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
        Optional<PriceResponse> response = priceClient.checkPriceByName(request.productName());
        if (response.isEmpty()) {
            throw new RuntimeException("Price not found");
        }
        return saveNewProductObservation(response.get());
    }

    @Transactional
    public ProductObservationResponse startObservingProductByUrl(ProductObservationByUrlRequest request) {
        Optional<PriceResponse> response = priceClient.checkPriceByUrl(request.productUrl());
        if (response.isEmpty()) {
            throw new RuntimeException("Price not found");
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

        productRepository.save(observation);
        return ProductObservationResponse.mapToDto(observation);
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
                .orElseThrow(() -> new RuntimeException("Product not found or access denied"));

        productRepository.delete(product);
    }
}