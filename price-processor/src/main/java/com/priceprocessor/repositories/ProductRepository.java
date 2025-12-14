package com.priceprocessor.repositories;

import com.priceprocessor.models.ProductObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductObservation, Long> {
    List<ProductObservation> findByUserEmail(String userEmail);
}