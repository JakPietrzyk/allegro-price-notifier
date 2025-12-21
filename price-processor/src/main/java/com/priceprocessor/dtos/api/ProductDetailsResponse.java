package com.priceprocessor.dtos.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailsResponse(
        Long id,
        String productName,
        String productUrl,
        BigDecimal currentPrice,
        String userEmail,
        List<PriceHistoryDto> priceHistory
) {
    public record PriceHistoryDto(BigDecimal price, LocalDateTime checkedAt) {}
}