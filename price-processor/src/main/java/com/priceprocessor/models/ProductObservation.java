package com.priceprocessor.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String productUrl;

    @Column(nullable = false)
    private String userEmail;

    private String currency;

    private Double currentPrice;

    private LocalDateTime lastCheckedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
