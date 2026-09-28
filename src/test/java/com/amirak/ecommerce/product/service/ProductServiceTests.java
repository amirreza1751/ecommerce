package com.amirak.ecommerce.product.service;

import com.amirak.ecommerce.product.dto.ProductRequest;
import com.amirak.ecommerce.product.dto.ProductResponse;
import com.amirak.ecommerce.product.entity.Product;
import com.amirak.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository products;

    @InjectMocks
    private ProductService service;

    @Test
    void createsProductWhenSkuIsAvailable() {
        ProductRequest request = request("SKU-1", "Keyboard");
        when(products.existsBySku("SKU-1")).thenReturn(false);
        when(products.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = service.create(request);

        assertEquals("SKU-1", response.sku());
        assertEquals("Keyboard", response.name());
        assertEquals(new BigDecimal("89.99"), response.price());
        verify(products).save(any(Product.class));
    }

    @Test
    void rejectsCreateWhenSkuAlreadyExists() {
        when(products.existsBySku("SKU-1")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.create(request("SKU-1", "Keyboard")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(products, never()).save(any(Product.class));
    }

    @Test
    void findsProductById() {
        when(products.findById(7L)).thenReturn(Optional.of(product("SKU-7", "Keyboard")));

        ProductResponse response = service.findById(7L);

        assertEquals("SKU-7", response.sku());
        assertEquals("Keyboard", response.name());
    }

    @Test
    void returnsNotFoundWhenProductDoesNotExist() {
        when(products.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.findById(404L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updatesProductWhenSkuIsNotUsedByAnotherProduct() {
        Product product = product("SKU-1", "Keyboard");
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(products.existsBySkuAndIdNot("SKU-2", 1L)).thenReturn(false);
        when(products.save(product)).thenReturn(product);

        ProductResponse response = service.update(1L, request("SKU-2", "Keyboard Pro"));

        assertEquals("SKU-2", response.sku());
        assertEquals("Keyboard Pro", response.name());
        verify(products).save(product);
    }

    @Test
    void rejectsUpdateWhenSkuBelongsToAnotherProduct() {
        when(products.findById(1L)).thenReturn(Optional.of(product("SKU-1", "Keyboard")));
        when(products.existsBySkuAndIdNot("SKU-2", 1L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, request("SKU-2", "Keyboard Pro")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(products, never()).save(any(Product.class));
    }

    @Test
    void searchesByNameOrSkuAndPreservesPageMetadata() {
        Pageable pageable = PageRequest.of(1, 5);
        var productsOnPage = java.util.List.of(
                product("SKU-KEY-1", "Keyboard 1"),
                product("SKU-KEY-2", "Keyboard 2"),
                product("SKU-KEY-3", "Keyboard 3"));
        when(products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase("key", "key", pageable))
                .thenReturn(new PageImpl<>(productsOnPage, pageable, 8));

        var result = service.findAll(" key ", pageable);

        assertEquals(8, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(3, result.getNumberOfElements());
        assertEquals("SKU-KEY-1", result.getContent().getFirst().sku());
    }

    @Test
    void listsWithoutSearchUsingRequestedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(products.findAll(pageable)).thenReturn(new PageImpl<>(java.util.List.of(product("SKU-1", "Keyboard"))));

        var result = service.findAll("  ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(products).findAll(pageable);
    }

    @Test
    void deletesExistingProduct() {
        Product product = product("SKU-1", "Keyboard");
        when(products.findById(1L)).thenReturn(Optional.of(product));

        service.delete(1L);

        verify(products).delete(product);
    }

    @Test
    void refusesToDeleteMissingProduct() {
        when(products.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.delete(404L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(products, never()).delete(any(Product.class));
    }

    private static ProductRequest request(String sku, String name) {
        return new ProductRequest(sku, name, "Description", new BigDecimal("89.99"), "EUR");
    }

    private static Product product(String sku, String name) {
        return Product.create(sku, name, "Description", new BigDecimal("89.99"), "EUR");
    }
}
