package com.priceprocessor.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_observations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, length = 1000)
    private String productUrl;

    @Column(nullable = false)
    private String userEmail;

    private BigDecimal currentPrice;

    private LocalDateTime lastCheckedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "productObservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PriceHistory> priceHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void addPriceHistory(BigDecimal price, LocalDateTime date) {
        PriceHistory history = PriceHistory.builder()
                .price(price)
                .checkedAt(date)
                .productObservation(this)
                .build();
        this.priceHistory.add(history);
        this.currentPrice = price;
        this.lastCheckedAt = date;
    }
}