package com.amirak.ecommerce.product.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
