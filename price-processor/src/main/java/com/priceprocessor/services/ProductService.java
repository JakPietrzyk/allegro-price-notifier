package com.priceprocessor.services;

import com.priceprocessor.dtos.api.ProductObservationRequest;
import com.priceprocessor.dtos.api.ProductObservationResponse;
import com.priceprocessor.dtos.crawler.PriceResponse;
import com.priceprocessor.models.ProductObservation;
import com.priceprocessor.repositories.ProductRepository;
import com.priceprocessor.services.interfaces.PriceClient;
import com.priceprocessor.services.queue.NotificationProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final PriceClient priceClient;
    private final NotificationProducer notificationProducer;
    private final Counter priceCheckCounter;

    public ProductService(ProductRepository productRepository, PriceClient priceClient,
                          NotificationProducer notificationProducer, MeterRegistry registry) {
        this.productRepository = productRepository;
        this.priceClient = priceClient;
        this.notificationProducer = notificationProducer;
        this.priceCheckCounter = Counter.builder("product.price.checked")
                .description("Number of times price was checked")
                .register(registry);

        this.priceCheckCounter.increment();
    }

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