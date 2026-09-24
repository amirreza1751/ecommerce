package com.amirak.ecommerce.product.service;

import com.amirak.ecommerce.product.dto.ProductRequest;
import com.amirak.ecommerce.product.dto.ProductResponse;
import com.amirak.ecommerce.product.entity.Product;
import com.amirak.ecommerce.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public Page<ProductResponse> findAll(String query, Pageable pageable) {
        Page<Product> page;
        if (query == null || query.isBlank()) {
            page = products.findAll(pageable);
        } else {
            page = products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
                    query.trim(), query.trim(), pageable);
        }
        return page.map(ProductService::toResponse);
    }

    public ProductResponse findById(Long id) {
        return toResponse(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        }

        Product product = new Product(request.sku(), request.name(), request.description(), request.price(), request.currency());
        return toResponse(products.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        if (products.existsBySkuAndIdNot(request.sku(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
        }
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency());
        return toResponse(products.save(product));
    }

    @Transactional
    public void delete(Long id) {
        products.delete(findProduct(id));
    }

    private Product findProduct(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}


