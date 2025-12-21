package com.priceprocessor.repositories;

import com.priceprocessor.models.ProductObservation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<ProductObservation, Long> {
    List<ProductObservation> findByUserEmail(String userEmail);
    @Query("SELECT p FROM ProductObservation p ORDER BY p.lastCheckedAt ASC NULLS FIRST")
    List<ProductObservation> findProductsToUpdate(Pageable pageable);
}