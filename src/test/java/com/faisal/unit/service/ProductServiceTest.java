package com.faisal.unit.service;

import com.faisal.dto.CachedPage;
import com.faisal.dto.request.CreateProductRequest;
import com.faisal.dto.response.ProductResponse;
import com.faisal.exception.BadRequestException;
import com.faisal.exception.ResourceNotFoundException;
import com.faisal.mapper.ProductMapper;
import com.faisal.model.Product;
import com.faisal.repository.ProductRepository;
import com.faisal.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    /* ---------------- CREATE ---------------- */

    @Test
    void create_shouldSaveAndReturnProductResponse() {
        CreateProductRequest request = mock(CreateProductRequest.class);
        Product product = mock(Product.class);
        Product savedProduct = mock(Product.class);
        ProductResponse response = mock(ProductResponse.class);

        when(productMapper.fromCreate(request)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(response);

        ProductResponse result = productService.create(request);

        assertThat(result).isSameAs(response);
        verify(productRepository).save(product);
    }

    /* ---------------- GET BY ID ---------------- */

    @Test
    void getById_shouldReturnProduct_whenExistsAndNotDeleted() {
        Product product = mock(Product.class);
        ProductResponse response = mock(ProductResponse.class);

        when(product.isDeleted()).thenReturn(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getById(1L);

        assertThat(result).isSameAs(response);
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
    }

    /* ---------------- LIST ---------------- */

    @Test
    void list_shouldReturnPagedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = mock(Product.class);
        ProductResponse response = mock(ProductResponse.class);

        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByDeletedFalse(pageable)).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.list(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    /* ---------------- SEARCH ---------------- */

    @Test
    void search_shouldThrowBadRequest_whenMinPriceGreaterThanMaxPrice() {
        assertThatThrownBy(() ->
                productService.search(
                        "test",
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(50),
                        true,
                        Pageable.unpaged()
                )
        ).isInstanceOf(BadRequestException.class)
                .hasMessage("minPrice must be <= maxPrice");
    }

    @Test
    void searchCached_shouldReturnCachedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        ProductResponse response = mock(ProductResponse.class);
        Page<ProductResponse> page =
                new PageImpl<>(List.of(response), pageable, 1);

        ProductService spy = spy(productService);
        doReturn(page).when(spy).search(any(), any(), any(), any(), any());

        CachedPage<ProductResponse> result =
                spy.searchCached("name", null, null, null, pageable);

        assertThat(result.content()).containsExactly(response);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    /* ---------------- UPDATE ---------------- */

    @Test
    void update_shouldModifyAndReturnProduct() {
        CreateProductRequest request = mock(CreateProductRequest.class);
        Product product = mock(Product.class);
        Product saved = mock(Product.class);
        ProductResponse response = mock(ProductResponse.class);

        when(product.isDeleted()).thenReturn(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(saved);
        when(productMapper.toResponse(saved)).thenReturn(response);

        ProductResponse result = productService.update(1L, request);

        verify(productMapper).update(product, request);
        assertThat(result).isSameAs(response);
    }

    /* ---------------- DELETE ---------------- */

    @Test
    void delete_shouldSoftDeleteProduct() {
        Product product = mock(Product.class);

        when(product.isDeleted()).thenReturn(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L);

        verify(product).softDelete();
        verify(productRepository).save(product);
    }

    /* ---------------- RESERVE STOCK ---------------- */

    @Test
    void reserveStock_shouldDecreaseQuantityAndReturnPrice() {
        Product product = mock(Product.class);

        when(product.isDeleted()).thenReturn(false);
        when(product.getQuantity()).thenReturn(10);
        when(product.getPrice()).thenReturn(BigDecimal.TEN);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        BigDecimal price = productService.reserveStock(1L, 3);

        verify(product).setQuantity(7);
        verify(productRepository).save(product);
        assertThat(price).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void reserveStock_shouldThrowException_whenQuantityInvalid() {
        assertThatThrownBy(() ->
                productService.reserveStock(1L, 0)
        ).isInstanceOf(BadRequestException.class)
                .hasMessage("Quantity must be at least 1");
    }

    @Test
    void reserveStock_shouldThrowException_whenInsufficientStock() {
        Product product = mock(Product.class);

        when(product.isDeleted()).thenReturn(false);
        when(product.getQuantity()).thenReturn(2);
        when(product.getName()).thenReturn("Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() ->
                productService.reserveStock(1L, 5)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }
}
