package et.kifiya.promoquoter.UnitTestServices;

import et.kifiya.promoquoter.enums.Category;
import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.promotion.PromotionStrategy;
import et.kifiya.promoquoter.repository.PromotionRepository;
import et.kifiya.promoquoter.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock private PromotionRepository promotionRepository;
    @Mock private PromotionStrategy strategy1;
    @Mock private PromotionStrategy strategy2;

    private PromotionService promotionService;
    private Product product;
    private Promotion promotion;

    @BeforeEach
    void setUp() {
        promotionService = new PromotionService(promotionRepository, List.of(strategy1, strategy2));

        product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Test Product");
        product.setCategory(Category.ELECTRONICS);
        product.setPrice(new BigDecimal("100.00"));

        promotion = new Promotion();
        promotion.setId(UUID.randomUUID());
        promotion.setName("Test Promotion");
        promotion.setType(PromotionType.PERCENT_OFF_CATEGORY);
        promotion.setActive(true);
    }

    @Test
    void applyPromotions_WithSupportedPromotion_ShouldApplyDiscount() {
        // Arrange
        Map<String, Product> products = Map.of(product.getId().toString(), product);
        Map<String, Integer> cartItems = Map.of(product.getId().toString(), 2);

        PromotionStrategy.PromotionResult expectedResult =
                new PromotionStrategy.PromotionResult(new BigDecimal("20.00"), "10% discount");

        when(strategy1.supports(promotion)).thenReturn(true);
        when(strategy1.apply(promotion, products, cartItems)).thenReturn(expectedResult);

        // Act
        PromotionService.PromotionContext context =
                promotionService.applyPromotions(List.of(promotion), products, cartItems);

        // Assert
        assertEquals(new BigDecimal("20.00"), context.getTotalDiscount());
        assertEquals(1, context.getAppliedPromotions().size());
    }

    @Test
    void applyPromotions_WithMultiplePromotions_ShouldApplyInPriorityOrder() {
        // Arrange
        Map<String, Product> products = Map.of(product.getId().toString(), product);
        Map<String, Integer> cartItems = Map.of(product.getId().toString(), 2);

        Promotion highPriority = new Promotion();
        highPriority.setId(UUID.randomUUID());
        highPriority.setPriority(1);
        highPriority.setActive(true);

        Promotion lowPriority = new Promotion();
        lowPriority.setId(UUID.randomUUID());
        lowPriority.setPriority(2);
        lowPriority.setActive(true);

        when(strategy1.supports(any())).thenReturn(true);
        when(strategy1.apply(any(), any(), any()))
                .thenReturn(new PromotionStrategy.PromotionResult(new BigDecimal("10.00"), "Discount"));

        // Act
        promotionService.applyPromotions(List.of(lowPriority, highPriority), products, cartItems);

        // Assert - Should process high priority first
        verify(strategy1).supports(highPriority);
        verify(strategy1).supports(lowPriority);
    }
}
