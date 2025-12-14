package com.priceprocessor.services;

import com.priceprocessor.dtos.api.ProductObservationRequest;
import com.priceprocessor.dtos.api.ProductObservationResponse;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.interfaces.PriceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceClient priceClient;

    public ProductObservationResponse startObservingProductByName(ProductObservationRequest request) {
        Optional<PriceResponse> response = priceClient.checkPriceByName(request.productName());
        if (!response.isPresent()) {
            //THROW SOMETHING
        }
        PriceResponse priceResponse = response.get();

        ProductObservation observation = ProductObservation.builder()
                .productName(priceResponse.foundProductName())
                .productUrl(priceResponse.ceneoUrl())
                .userEmail(request.userEmail())
                .currentPrice(priceResponse.price())
                .build();

        productRepository.save(observation);
        return ProductObservationResponse
                .mapToDto(observation);
    }

    public ProductObservationResponse startObservingProductByUrl(ProductObservationRequest request) {
        Optional<PriceResponse> response = priceClient.checkPriceByUrl(request.productUrl());
        if (!response.isPresent()) {
            //THROW SOMETHING
        }
        PriceResponse priceResponse = response.get();

        ProductObservation observation = ProductObservation.builder()
                .productName(priceResponse.foundProductName())
                .productUrl(priceResponse.ceneoUrl())
                .userEmail(request.userEmail())
                .currentPrice(priceResponse.price())
                .build();

        productRepository.save(observation);

        return ProductObservationResponse
                .mapToDto(observation);
    }

    public List<ProductObservationResponse> getAllObservedProducts() {
        List<ProductObservation> observedProducts = productRepository.findAll();

        return observedProducts.stream()
                .map(ProductObservationResponse::mapToDto)
                .toList();
    }
}