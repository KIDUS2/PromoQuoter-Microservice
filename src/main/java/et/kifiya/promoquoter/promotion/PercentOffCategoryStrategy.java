package et.kifiya.promoquoter.promotion;



import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Component
@Slf4j
public class PercentOffCategoryStrategy implements PromotionStrategy {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Override
    public boolean supports(Promotion promotion) {
        boolean supports = promotion.getType() == PromotionType.PERCENT_OFF_CATEGORY
                && promotion.getCategory() != null
                && promotion.getDiscountPercent() != null
                && promotion.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0;

        log.info("PercentOffCategoryStrategy.supports({}) = {}", promotion.getName(), supports);
        return supports;
    }

    @Override
    public PromotionResult apply(Promotion promotion, Map<String, Product> products,
                                 Map<String, Integer> cartItems) {
        log.info("Applying PercentOffCategoryStrategy for promotion: {}", promotion.getName());

        BigDecimal totalDiscount = BigDecimal.ZERO;
        StringBuilder description = new StringBuilder();
        int affectedItems = 0;

        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            String productId = entry.getKey();
            Integer quantity = entry.getValue();
            Product product = products.get(productId);

            if (isProductEligible(product, promotion)) {
                BigDecimal itemTotal = calculateItemTotal(product, quantity);
                BigDecimal discount = calculateDiscount(itemTotal, promotion.getDiscountPercent());
                totalDiscount = totalDiscount.add(discount);
                affectedItems++;

                if (description.length() == 0) {
                    description.append(String.format("%s%% off %s category",
                            promotion.getDiscountPercent().setScale(0, RoundingMode.HALF_UP),
                            promotion.getCategory()));
                }
            }
        }

        if (affectedItems > 0) {
            description.append(String.format(" (applied to %d items)", affectedItems));
        }

        log.info("PercentOffCategoryStrategy result - Discount: {}, Description: {}",
                totalDiscount, description.toString());

        return new PromotionResult(totalDiscount, description.toString());
    }

    private boolean isProductEligible(Product product, Promotion promotion) {
        return product != null
                && product.getCategory().name().equals(promotion.getCategory())
                && product.getPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateItemTotal(Product product, Integer quantity) {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    private BigDecimal calculateDiscount(BigDecimal itemTotal, BigDecimal discountPercent) {
        return itemTotal.multiply(discountPercent)
                .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
