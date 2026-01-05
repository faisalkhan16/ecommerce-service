package com.faisal.service;

import com.faisal.dto.CachedPage;
import com.faisal.exception.BadRequestException;
import com.faisal.exception.ResourceNotFoundException;
import com.faisal.dto.request.CreateProductRequest;
import com.faisal.dto.response.ProductResponse;
import com.faisal.mapper.ProductMapper;
import com.faisal.model.Product;
import com.faisal.repository.ProductRepository;
import com.faisal.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @CacheEvict(cacheNames = {"productsList", "productById","productsSearch"}, allEntries = true)
    public ProductResponse create(CreateProductRequest request) {
        Product product = productMapper.fromCreate(request);
        Product saved = productRepository.save(product);
        log.info("Created product id={}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Cacheable(
            cacheNames = "productById",
            key = "#id"
    )
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return productMapper.toResponse(product);
    }

    @Cacheable(
            cacheNames = "productsList",
            key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()"
    )
    public Page<ProductResponse> list(Pageable pageable) {
        return productRepository.findByDeletedFalse(pageable).map(productMapper::toResponse);
    }

    @Cacheable(
            cacheNames = "productsSearch",
            key = "(#name == null ? '' : #name.trim().toLowerCase()) + ':' + " +
                    "(#minPrice == null ? '' : #minPrice.toPlainString()) + ':' + " +
                    "(#maxPrice == null ? '' : #maxPrice.toPlainString()) + ':' + " +
                    "(#available == null ? '' : #available.toString()) + ':' + " +
                    "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()"
    )
    public CachedPage<ProductResponse> searchCached(String name, BigDecimal minPrice, BigDecimal maxPrice, Boolean available, Pageable pageable) {
        Page<ProductResponse> page = search(name, minPrice, maxPrice, available, pageable);
        return new CachedPage<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public Page<ProductResponse> search(String name, BigDecimal minPrice, BigDecimal maxPrice, Boolean available, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must be <= maxPrice");
        }

        Specification<Product> spec = ProductSpecifications.notDeleted();

        if (name != null && !name.isBlank()) {
            spec = spec.and(ProductSpecifications.nameContainsIgnoreCase(name));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.priceGte(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceLte(maxPrice));
        }
        if (available != null) {
            spec = spec.and(ProductSpecifications.available(available));
        }

        return productRepository.findAll(spec, pageable).map(productMapper::toResponse);
    }

    public Page<ProductResponse> searchForApi(String name, BigDecimal minPrice, BigDecimal maxPrice, Boolean available, Pageable pageable) {
        CachedPage<ProductResponse> cached = searchCached(name, minPrice, maxPrice, available, pageable);
        return new PageImpl<>(cached.content(), pageable, cached.totalElements());
    }

    @Transactional
    @CacheEvict(cacheNames = {"productsList", "productById","productsSearch"}, allEntries = true)
    public ProductResponse update(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productMapper.update(product, request);
        Product saved = productRepository.save(product);
        log.info("Updated product id={}", id);
        return productMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {"productsList", "productById","productsSearch"}, allEntries = true)
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.softDelete();
        productRepository.save(product);
        log.info("Soft deleted product id={}", id);
    }


    @Transactional
    @CacheEvict(cacheNames = {"productsList", "productById","productsSearch"}, allEntries = true)
    public BigDecimal reserveStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        // First DB call: fetch the product to check existence, deletion status, and available stock.
        // We need this because we cannot reduce stock for a product that doesn't exist or is deleted.
        Product product = productRepository.findById(productId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));

        if (product.getQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product: " + product.getName());
        }

        // Second DB call: update the stock by subtracting the requested quantity.
        // This is done in a separate call (via save) to ensure that the quantity in the database reflects
        // the reservation atomically. We cannot rely on the quantity in the OrderItemRequest because
        // the request does not contain the actual price or stock details of the product.
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        // Return the price of the product. This requires fetching it from the DB because
        // during order booking, the price is not available in the OrderItemRequest.
        log.info("Reserved stock productId={}, qty={}, price={}", productId, quantity, product.getPrice());
        return product.getPrice();
    }

}