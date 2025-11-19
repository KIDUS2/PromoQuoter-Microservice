package et.kifiya.promoquoter.promotion;



import et.kifiya.promoquoter.model.CartItem;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.model.Quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PromotionStrategy {
    /**
     * Checks if this strategy supports the given promotion type
     * @param promotion the promotion to check
     * @return true if this strategy can handle the promotion
     */
    boolean supports(Promotion promotion);

    /**
     * Applies the promotion to the cart items and returns the discount result
     * @param promotion the promotion to apply
     * @param products map of productId to Product entities
     * @param cartItems map of productId to quantity in cart
     * @return PromotionResult containing discount amount and description
     */
    PromotionResult apply(Promotion promotion, Map<String, Product> products,
                          Map<String, Integer> cartItems);

    /**
     * Result of applying a promotion
     */
    class PromotionResult {
        private final BigDecimal discount;
        private final String description;

        public PromotionResult(BigDecimal discount, String description) {
            this.discount = discount != null ? discount : BigDecimal.ZERO;
            this.description = description != null ? description : "";
        }

        public BigDecimal getDiscount() { return discount; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format("PromotionResult{discount=%s, description='%s'}", discount, description);
        }
    }
}

