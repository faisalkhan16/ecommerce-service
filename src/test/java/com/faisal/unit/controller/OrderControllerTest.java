package com.faisal.unit.controller;

import com.faisal.controller.OrderController;
import com.faisal.dto.request.CreateOrderRequest;
import com.faisal.dto.request.OrderItemRequest;
import com.faisal.dto.response.OrderResponse;
import com.faisal.enums.Role;
import com.faisal.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private ObjectMapper objectMapper;

    private Jwt jwt;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("userId", 5L)
                .claim("roles", List.of("USER"))
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalJwtResolver(() -> jwt)
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void placeOrder_shouldReturn201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 2))
        );

        OrderResponse response = new OrderResponse(
                10L,
                5L,
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(20),
                Instant.now(),
                List.of()
        );

        when(orderService.placeOrder(
                5L,
                Role.USER,
                request.items()
        )).thenReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.order_id").value(10L))
                .andExpect(jsonPath("$.data.user_id").value(5L));
    }


    static final class AuthenticationPrincipalJwtResolver implements HandlerMethodArgumentResolver {
        private final java.util.function.Supplier<Jwt> jwtSupplier;

        AuthenticationPrincipalJwtResolver(java.util.function.Supplier<Jwt> jwtSupplier) {
            this.jwtSupplier = jwtSupplier;
        }

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && Jwt.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            return jwtSupplier.get();
        }
    }
}