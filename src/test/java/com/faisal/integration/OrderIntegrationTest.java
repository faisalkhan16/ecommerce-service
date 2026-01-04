package com.faisal.integration;

import com.faisal.config.RolesClaimConverter;
import com.faisal.dto.request.CreateOrderRequest;
import com.faisal.dto.request.OrderItemRequest;
import com.faisal.enums.Role;
import com.faisal.model.Product;
import com.faisal.model.User;
import com.faisal.repository.OrderRepository;
import com.faisal.repository.ProductRepository;
import com.faisal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .name("Customer")
                .email("customer@example.com")
                .role(Role.USER)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Gadget")
                .description("Cool gadget")
                .price(BigDecimal.valueOf(100))
                .quantity(10)
                .build());
    }

    @Test
    void placeOrder_shouldSucceed() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest(testProduct.getId(), 2);
        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(itemRequest));

        mockMvc.perform(post("/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(Objects.requireNonNull(new RolesClaimConverter().convert(
                                        Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("roles", List.of("USER"))
                                                .build()
                                )))
                                .jwt(jwt -> jwt
                                        .claim("userId", testUser.getId())
                                        .claim("roles", List.of("USER"))
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.total").value(200.0));

        assertThat(orderRepository.findAll()).hasSize(1);
        
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getQuantity()).isEqualTo(8);
    }

    @Test
    void placeOrder_shouldSucceed_applyDiscountForPremiumUser() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest(testProduct.getId(), 2);
        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(itemRequest));

        mockMvc.perform(post("/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(Objects.requireNonNull(new RolesClaimConverter().convert(
                                        Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("roles", List.of("PREMIUM_USER"))
                                                .build()
                                )))
                                .jwt(jwt -> jwt
                                        .claim("userId", testUser.getId())
                                        .claim("roles", List.of("PREMIUM_USER"))
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user_id").value(testUser.getId()))
                .andExpect(jsonPath("$.data.total").value(180.0))
                .andExpect(jsonPath("$.data.total_discount").value(20.0));


        assertThat(orderRepository.findAll()).hasSize(1);

        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getQuantity()).isEqualTo(8);
    }

    @Test
    void placeOrder_insufficientStock_shouldFail() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest(testProduct.getId(), 20);
        CreateOrderRequest orderRequest = new CreateOrderRequest(List.of(itemRequest));

        mockMvc.perform(post("/orders")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(Objects.requireNonNull(new RolesClaimConverter().convert(
                                        Jwt.withTokenValue("token")
                                                .header("alg", "none")
                                                .claim("roles", List.of("USER"))
                                                .build()
                                )))
                                .jwt(jwt -> jwt
                                        .claim("userId", testUser.getId())
                                        .claim("roles", List.of("USER"))
                                )
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }
}
