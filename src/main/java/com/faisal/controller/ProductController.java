package com.faisal.controller;

import com.faisal.dto.request.CreateProductRequest;
import com.faisal.dto.request.PageRequest;
import com.faisal.dto.response.ApiResponse;
import com.faisal.dto.response.ProductResponse;
import com.faisal.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Products management endpoints")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create product",
            description = "Creates a new product. ADMIN only.",
            operationId = "createProduct"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Parameter(description = "Product creation payload.")
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.create(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('USER','PREMIUM_USER','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get product by id",
            description = "Returns a single non-deleted product by id.",
            operationId = "getProductById"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> get(
            @Parameter(description = "Product id.", example = "1")
            @PathVariable Long id
    ) {
        ProductResponse response = productService.getById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('USER','PREMIUM_USER','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Search products",
            description = "Search products by combinable filters (name, price range, availability) with paging/sorting.",
            operationId = "searchProducts"
    )
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(
            @RequestParam(required = false, name = "name") String name,
            @RequestParam(required = false , name = "min_price") BigDecimal minPrice,
            @RequestParam(required = false, name = "max_price") BigDecimal maxPrice,
            @RequestParam(required = false,name = "is_available") Boolean available,
            @RequestParam(defaultValue = "0", name = "page") int page,
            @RequestParam(defaultValue = "10", name = "size") int size,
            @RequestParam(defaultValue = "id", name = "sort_by") String sortBy,
            @RequestParam(defaultValue = "ASC", name = "direction") String direction
    ) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.fromString(direction.toUpperCase()), sortBy)
        );
        Page<ProductResponse> response = productService.searchForApi(name, minPrice, maxPrice, available, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update product",
            description = "Updates an existing product by id. ADMIN only.",
            operationId = "updateProduct"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @Parameter(description = "Product id to update.", example = "1")
            @PathVariable Long id,

            @Parameter(description = "Product update payload.")
            @RequestBody CreateProductRequest request
    ) {
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete product",
            description = "Soft deletes a product by id. ADMIN only.",
            operationId = "deleteProduct"
    )
    public ResponseEntity<ApiResponse<String>> delete(
            @Parameter(description = "Product id to delete.", example = "1")
            @PathVariable Long id
    ) {
        productService.delete(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Product deleted successfully"));
    }
}