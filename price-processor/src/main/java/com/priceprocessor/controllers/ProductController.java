package com.priceprocessor.controllers;

import com.priceprocessor.dtos.api.*;
import com.priceprocessor.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductObservationResponse>> getProducts() {
        return ResponseEntity.ok(productService.getAllObservedProducts());
    }

    @PostMapping("/search")
    public ResponseEntity<ProductObservationResponse> addProductByName(@RequestBody ProductObservationByNameRequest request) {
        ProductObservationResponse created = productService.startObservingProductByName(request);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/url")
    public ResponseEntity<ProductObservationResponse> addProductByUrl(@RequestBody ProductObservationByUrlRequest request) {
        ProductObservationResponse created = productService.startObservingProductByUrl(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailsResponse> getProductDetails(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductDetails(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteObservedProduct(id);
        return ResponseEntity.noContent().build();
    }
}