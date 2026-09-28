package com.amirak.ecommerce.product.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductTests {

    @Test
    void createsProductWithProvidedValues() {
        Product product = Product.create("SKU-1", "Keyboard", "Compact keyboard",
                new BigDecimal("89.99"), "EUR");

        assertEquals("SKU-1", product.getSku());
        assertEquals("Keyboard", product.getName());
        assertEquals("Compact keyboard", product.getDescription());
        assertEquals(new BigDecimal("89.99"), product.getPrice());
        assertEquals("EUR", product.getCurrency());
    }

    @Test
    void acceptsSchemaLengthAndPriceBoundaries() {
        Product product = Product.create("S".repeat(64), "N".repeat(200), null,
                new BigDecimal("9999999999.99"), "EUR");

        assertEquals(64, product.getSku().length());
        assertEquals(200, product.getName().length());
        assertEquals(new BigDecimal("9999999999.99"), product.getPrice());
    }

    @Test
    void rejectsBlankOrOversizedSku() {
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(" ", "Keyboard", null, new BigDecimal("1.00"), "EUR"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("S".repeat(65), "Keyboard", null, new BigDecimal("1.00"), "EUR"));
    }

    @Test
    void rejectsBlankOrOversizedName() {
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "", null, new BigDecimal("1.00"), "EUR"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "N".repeat(201), null, new BigDecimal("1.00"), "EUR"));
    }

    @Test
    void rejectsNegativeNullOrOutOfRangePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, new BigDecimal("-0.01"), "EUR"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, null, "EUR"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, new BigDecimal("1.001"), "EUR"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, new BigDecimal("10000000000.00"), "EUR"));
    }

    @Test
    void rejectsCurrencyThatIsNotThreeUppercaseLetters() {
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, new BigDecimal("1.00"), "eur"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, new BigDecimal("1.00"), "EU"));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create("SKU-1", "Keyboard", null, new BigDecimal("1.00"), null));
    }

    @Test
    void updatesAllMutableProductFields() {
        Product product = Product.create("SKU-1", "Keyboard", "Old description",
                new BigDecimal("89.99"), "EUR");

        product.updateDetails("SKU-2", "Keyboard Pro", null, new BigDecimal("99.00"), "USD");

        assertEquals("SKU-2", product.getSku());
        assertEquals("Keyboard Pro", product.getName());
        assertNull(product.getDescription());
        assertEquals(new BigDecimal("99.00"), product.getPrice());
        assertEquals("USD", product.getCurrency());
    }

    @Test
    void invalidUpdateDoesNotPartiallyChangeProduct() {
        Product product = Product.create("SKU-1", "Keyboard", null,
                new BigDecimal("89.99"), "EUR");

        assertThrows(IllegalArgumentException.class,
                () -> product.updateDetails("SKU-2", "Keyboard Pro", null,
                        new BigDecimal("-1.00"), "USD"));

        assertEquals("SKU-1", product.getSku());
        assertEquals("Keyboard", product.getName());
        assertEquals(new BigDecimal("89.99"), product.getPrice());
        assertEquals("EUR", product.getCurrency());
    }
}

