package com.priceprocessor.services.clients;

import com.priceprocessor.dtos.crawler.PriceResponse;

import java.util.Optional;

public interface PriceClient {
    Optional<PriceResponse> checkPriceByName(String productName);
    Optional<PriceResponse> checkPriceByUrl(String productUrl);
}
