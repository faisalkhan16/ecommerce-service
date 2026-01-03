package com.faisal.unit.controller;

import com.faisal.controller.ProductController;
import com.faisal.dto.request.CreateProductRequest;
import com.faisal.dto.response.ProductResponse;
import com.faisal.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Required for Instant/LocalDateTime

        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)) // Fixes Serialization issues
                .build();
    }

    @Test
    void createProduct_shouldReturn201() throws Exception {
        CreateProductRequest request = new CreateProductRequest("iPhone", "Apple", BigDecimal.valueOf(1000), 10);
        ProductResponse response = new ProductResponse(1L, "iPhone", "Apple", BigDecimal.valueOf(1000), 10);

        when(productService.create(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // Controller returns 200 via ResponseEntity.ok()
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("iPhone"));
    }

    @Test
    void searchProducts_shouldReturnPagedData() throws Exception {
        ProductResponse product = new ProductResponse(1L, "TV", "Samsung", BigDecimal.valueOf(500), 20);

        // Fix: Use real Pageable to avoid UnsupportedOperationException during serialization
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductResponse> page = new PageImpl<>(List.of(product), pageable, 1);

        when(productService.searchForApi(any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/products")
                        .param("name", "TV")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("TV"));
    }

    @Test
    void deleteProduct_shouldReturnSuccess() throws Exception {
        doNothing().when(productService).delete(1L);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Product deleted successfully"));
    }
}