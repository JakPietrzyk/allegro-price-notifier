package com.priceprocessor.services;

import com.priceprocessor.dtos.api.ProductDetailsResponse;
import com.priceprocessor.dtos.api.ProductObservationRequest;
import com.priceprocessor.dtos.api.ProductObservationResponse;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.interfaces.PriceClient;
import com.priceprocessor.services.queue.NotificationProducer;
import lombok.RequiredArgsConstructor;
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
    private final NotificationProducer notificationProducer;

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(Long id) {
        ProductObservation product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

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
    public ProductObservationResponse startObservingProductByName(ProductObservationRequest request) {
        Optional<PriceResponse> response = priceClient.checkPriceByName(request.productName());
        if (response.isEmpty()) {
            throw new RuntimeException("Price not found");
        }
        return saveNewProductObservation(request, response.get());
    }

    @Transactional
    public ProductObservationResponse startObservingProductByUrl(ProductObservationRequest request) {
        Optional<PriceResponse> response = priceClient.checkPriceByUrl(request.productUrl());
        if (response.isEmpty()) {
            throw new RuntimeException("Price not found");
        }
        return saveNewProductObservation(request, response.get());
    }

    private ProductObservationResponse saveNewProductObservation(ProductObservationRequest request, PriceResponse priceResponse) {
        ProductObservation observation = ProductObservation.builder()
                .productName(priceResponse.foundProductName())
                .productUrl(priceResponse.ceneoUrl())
                .userEmail(request.userEmail())
                .build();

        observation.addPriceHistory(priceResponse.price(), LocalDateTime.now());

        productRepository.save(observation);
        return ProductObservationResponse.mapToDto(observation);
    }

    public List<ProductObservationResponse> getAllObservedProducts() {
        return productRepository.findAll().stream()
                .map(ProductObservationResponse::mapToDto)
                .toList();
    }

    public void deleteObservedProduct(Long id) {
        productRepository.deleteById(id);
    }
}