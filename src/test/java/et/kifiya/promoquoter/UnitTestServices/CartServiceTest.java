package et.kifiya.promoquoter.UnitTestServices;

import et.kifiya.promoquoter.dto.CartRequest;
import et.kifiya.promoquoter.dto.ResponseDTO.CartConfirmResponse;
import et.kifiya.promoquoter.dto.ResponseDTO.CartResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.CartConfirmRequest;
import et.kifiya.promoquoter.dto.requestDTO.CartItemRequest;
import et.kifiya.promoquoter.enums.Category;
import et.kifiya.promoquoter.enums.CustomerSegment;
import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Order;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.repository.OrderRepository;
import et.kifiya.promoquoter.service.CartService;
import et.kifiya.promoquoter.service.IdempotencyService;
import et.kifiya.promoquoter.service.ProductService;
import et.kifiya.promoquoter.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private ProductService productService;
    @Mock private PromotionService promotionService;
    @Mock private OrderRepository orderRepository;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks private CartService cartService;

    private Product product1, product2;
    private Promotion promotion1, promotion2;

    @BeforeEach
    void setUp() {
        product1 = new Product();
        product1.setId(UUID.randomUUID());
        product1.setName("Test Product 1");
        product1.setCategory(Category.ELECTRONICS);
        product1.setPrice(new BigDecimal("100.00"));
        product1.setStock(10);

        product2 = new Product();
        product2.setId(UUID.randomUUID());
        product2.setName("Test Product 2");
        product2.setCategory(Category.CLOTHING);
        product2.setPrice(new BigDecimal("50.00"));
        product2.setStock(20);

        promotion1 = new Promotion();
        promotion1.setId(UUID.randomUUID());
        promotion1.setName("10% Off Electronics");
        promotion1.setType(PromotionType.PERCENT_OFF_CATEGORY);
        promotion1.setCategory("ELECTRONICS");
        promotion1.setDiscountPercent(new BigDecimal("10.0"));
        promotion1.setActive(true);

        promotion2 = new Promotion();
        promotion2.setId(UUID.randomUUID());
        promotion2.setName("Buy 2 Get 1 Free");
        promotion2.setType(PromotionType.BUY_X_GET_Y);
        promotion2.setProductId(product2.getId());
        promotion2.setBuyQuantity(2);
        promotion2.setGetQuantity(1);
        promotion2.setActive(true);
    }

    @Test
    void calculateQuote_WithValidRequest_ShouldReturnQuote() {
        // Arrange
        CartRequest request = new CartRequest();
        CartItemRequest item1 = new CartItemRequest();
        item1.setProductId(product1.getId().toString());
        item1.setQty(2);
        request.setItems(List.of(item1));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1));
        when(promotionService.getActivePromotions()).thenReturn(List.of(promotion1));

        PromotionService.PromotionContext context = mock(PromotionService.PromotionContext.class);
        when(context.getTotalDiscount()).thenReturn(new BigDecimal("20.00"));
        when(context.getAppliedPromotions()).thenReturn(List.of());
        when(promotionService.applyPromotions(anyList(), anyMap(), anyMap())).thenReturn(context);

        // Act
        CartResponseDto result = cartService.calculateQuote(request);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("200.00"), result.getSubtotal());
        assertEquals(new BigDecimal("20.00"), result.getTotalDiscount());
        assertNotNull(result.getQuoteId());
    }

    @Test
    void confirmCart_WithNewIdempotencyKey_ShouldCreateOrder() {
        // Arrange
        CartConfirmRequest request = new CartConfirmRequest();
        CartItemRequest item = new CartItemRequest();
        item.setProductId(product1.getId().toString());
        item.setQty(1);
        request.setItems(List.of(item));
        request.setCustomerSegment(CustomerSegment.REGULAR);

        when(idempotencyService.getProcessedOrderId(anyString())).thenReturn(Optional.empty());
        when(productService.getProductsByIds(anyList())).thenReturn(List.of(product1));
        when(promotionService.getActivePromotions()).thenReturn(List.of());

        PromotionService.PromotionContext context = mock(PromotionService.PromotionContext.class);
        when(context.getTotalDiscount()).thenReturn(BigDecimal.ZERO);
        when(context.getAppliedPromotions()).thenReturn(List.of());
        when(promotionService.applyPromotions(anyList(), anyMap(), anyMap())).thenReturn(context);

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setStatus("CONFIRMED");
        savedOrder.setOrderDate(LocalDateTime.now());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        CartConfirmResponse result = cartService.confirmCart(request, "test-key");

        // Assert
        assertNotNull(result);
        assertEquals(savedOrder.getId(), result.getOrderId());
        verify(idempotencyService).storeIdempotencyKey("test-key", savedOrder.getId().toString());
    }

    @Test
    void confirmCart_WithExistingIdempotencyKey_ShouldReturnExistingOrder() {
        // Arrange
        CartConfirmRequest request = new CartConfirmRequest();
        String existingOrderId = UUID.randomUUID().toString();

        when(idempotencyService.getProcessedOrderId("existing-key")).thenReturn(Optional.of(existingOrderId));

        Order existingOrder = new Order();
        existingOrder.setId(UUID.fromString(existingOrderId));
        existingOrder.setStatus("CONFIRMED");
        when(orderRepository.findById(any(UUID.class))).thenReturn(Optional.of(existingOrder));

        // Act
        CartConfirmResponse result = cartService.confirmCart(request, "existing-key");

        // Assert
        assertNotNull(result);
        assertEquals(existingOrder.getId(), result.getOrderId());
        verify(orderRepository, never()).save(any(Order.class));
    }

}
