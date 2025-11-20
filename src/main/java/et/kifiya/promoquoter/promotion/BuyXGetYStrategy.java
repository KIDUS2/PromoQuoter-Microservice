package et.kifiya.promoquoter.promotion;


import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@Slf4j
public class BuyXGetYStrategy implements PromotionStrategy {

    @Override
    public boolean supports(Promotion promotion) {
        boolean supports = promotion.getType() == PromotionType.BUY_X_GET_Y
                && promotion.getProductId() != null
                && promotion.getBuyQuantity() != null
                && promotion.getBuyQuantity() > 0
                && promotion.getGetQuantity() != null
                && promotion.getGetQuantity() > 0;

        return supports;
    }

    @Override
    public PromotionResult apply(Promotion promotion, Map<String, Product> products,
                                 Map<String, Integer> cartItems) {

        String targetProductId = promotion.getProductId().toString();
        Integer quantityInCart = cartItems.get(targetProductId);

        if (quantityInCart == null || quantityInCart < promotion.getBuyQuantity()) {
            return new PromotionResult(BigDecimal.ZERO, "");
        }

        Product product = products.get(targetProductId);
        if (product == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return new PromotionResult(BigDecimal.ZERO, "");
        }
        int totalRequiredForOneFreeSet = promotion.getBuyQuantity() + promotion.getGetQuantity();
        int freeSets = quantityInCart / totalRequiredForOneFreeSet;

        if (freeSets == 0) {
            return new PromotionResult(BigDecimal.ZERO, "");
        }

        BigDecimal discount = calculateDiscount(product, freeSets, promotion.getGetQuantity());
        String description = buildDescription(product, promotion, freeSets);
        return new PromotionResult(discount, description);
    }

    private BigDecimal calculateDiscount(Product product, int freeSets, int getQuantity) {
        return product.getPrice().multiply(BigDecimal.valueOf(freeSets * getQuantity));
    }

    private String buildDescription(Product product, Promotion promotion, int freeSets) {
        return String.format("Buy %d Get %d Free - %s (%d free items)",
                promotion.getBuyQuantity(),
                promotion.getGetQuantity(),
                product.getName(),
                freeSets * promotion.getGetQuantity());
    }
}

