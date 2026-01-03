package com.faisal.integration;

import com.faisal.dto.request.CreateProductRequest;
import com.faisal.model.Product;
import com.faisal.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProduct_asAdmin_shouldSucceed() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Laptop", "High-end laptop", BigDecimal.valueOf(1500), 10);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Laptop"))
                .andExpect(jsonPath("$.data.price").value(1500));

        assertThat(productRepository.findAll()).hasSize(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    void createProduct_asUser_shouldBeForbidden() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Laptop", "High-end laptop", BigDecimal.valueOf(1500), 10);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getProduct_shouldReturnProduct() throws Exception {
        Product product = Product.builder()
                .name("Smartphone")
                .description("Latest smartphone")
                .price(BigDecimal.valueOf(800))
                .quantity(50)
                .build();
        product = productRepository.save(product);

        mockMvc.perform(get("/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Smartphone"))
                .andExpect(jsonPath("$.data.price").value(800));
    }

    @Test
    @WithMockUser(roles = "USER")
    void searchProducts_shouldReturnFilteredResults() throws Exception {
        productRepository.save(Product.builder().name("Apple").price(BigDecimal.valueOf(10)).quantity(100).build());
        productRepository.save(Product.builder().name("Banana").price(BigDecimal.valueOf(5)).quantity(200).build());
        productRepository.save(Product.builder().name("Cherry").price(BigDecimal.valueOf(15)).quantity(50).build());

        mockMvc.perform(get("/products")
                        .param("name", "a")
                        .param("min_price", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Apple"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateProduct_asAdmin_shouldSucceed() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Old Name")
                .description("Old Desc")
                .price(BigDecimal.valueOf(10))
                .quantity(5)
                .build());

        CreateProductRequest updateRequest = new CreateProductRequest("New Name", "New Desc", BigDecimal.valueOf(20), 10);

        mockMvc.perform(put("/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("New Name"))
                .andExpect(jsonPath("$.data.price").value(20));

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getPrice().stripTrailingZeros()).isEqualTo(BigDecimal.valueOf(20).stripTrailingZeros());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProduct_asAdmin_shouldSoftDelete() throws Exception {
        Product product = productRepository.save(Product.builder()
                .name("Delete Me")
                .description("Desc")
                .price(BigDecimal.valueOf(10))
                .quantity(5)
                .build());

        mockMvc.perform(delete("/products/" + product.getId()))
                .andExpect(status().isOk());

        Product deleted = productRepository.findById(product.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }
}
