package com.amirak.ecommerce.product;

import com.amirak.ecommerce.product.entity.Product;
import com.amirak.ecommerce.product.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTests {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository products;

    @BeforeEach
    void clearProducts() {
        products.deleteAll();
    }

    @Test
    void createsProductAndReturnsLocation() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("SKU-1", "Keyboard")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/products/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.sku").value("SKU-1"))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.price").value(89.99))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));
    }

    @Test
    void rejectsInvalidRequestWithFieldErrors() throws Exception {
        String invalidJson = """
                {
                  "sku": " ",
                  "name": "",
                  "description": null,
                  "price": -1.00,
                  "currency": "eur"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.sku", notNullValue()))
                .andExpect(jsonPath("$.errors.name", notNullValue()))
                .andExpect(jsonPath("$.errors.price", notNullValue()))
                .andExpect(jsonPath("$.errors.currency", notNullValue()));
    }

    @Test
    void rejectsDuplicateSkuWithConflict() throws Exception {
        products.save(Product.create("SKU-1", "Keyboard", null, new BigDecimal("89.99"), "EUR"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("SKU-1", "Another keyboard")))
                .andExpect(status().isConflict());
    }

    @Test
    void getsProductByIdAndReturnsNotFoundForUnknownId() throws Exception {
        Product product = products.save(Product.create(
                "SKU-1", "Keyboard", null, new BigDecimal("89.99"), "EUR"));

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-1"));

        mockMvc.perform(get("/api/products/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesProductAndRejectsAnotherProductsSku() throws Exception {
        Product product = products.save(Product.create(
                "SKU-1", "Keyboard", null, new BigDecimal("89.99"), "EUR"));
        products.save(Product.create("SKU-2", "Mouse", null, new BigDecimal("29.99"), "EUR"));

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("SKU-1", "Keyboard Pro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard Pro"));

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("SKU-2", "Keyboard Pro")))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/products/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validProductJson("SKU-3", "Missing")))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsProductsWithCaseInsensitiveSearchPaginationAndSorting() throws Exception {
        products.save(Product.create("SKU-A", "Alpha Keyboard", null, new BigDecimal("10.00"), "EUR"));
        products.save(Product.create("SKU-B", "Beta Keyboard", null, new BigDecimal("20.00"), "EUR"));
        products.save(Product.create("SKU-C", "Alpha Mouse", null, new BigDecimal("30.00"), "EUR"));

        mockMvc.perform(get("/api/products")
                        .param("q", "KEYBOARD")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-A"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/products")
                        .param("q", "keyboard")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SKU-B"));
    }

    @Test
    void searchesBySkuWhenNameDoesNotMatch() throws Exception {
        products.save(Product.create("SPECIAL-42", "Mouse", null, new BigDecimal("30.00"), "EUR"));

        mockMvc.perform(get("/api/products").param("q", "special-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("SPECIAL-42"));
    }

    @Test
    void deletesProductAndReturnsNotFoundAfterward() throws Exception {
        Product product = products.save(Product.create(
                "SKU-1", "Keyboard", null, new BigDecimal("89.99"), "EUR"));

        mockMvc.perform(delete("/api/products/{id}", product.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/products/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    private static String validProductJson(String sku, String name) {
        return """
                {
                  "sku": "%s",
                  "name": "%s",
                  "description": "Test description",
                  "price": 89.99,
                  "currency": "EUR"
                }
                """.formatted(sku, name);
    }
}

