package et.kifiya.promoquoter.promotion;


import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;

import java.math.BigDecimal;
import java.util.Map;

public interface PromotionStrategy {

    boolean supports(Promotion promotion);

    PromotionResult apply(Promotion promotion, Map<String, Product> products,
                          Map<String, Integer> cartItems);

    class PromotionResult {
        private final BigDecimal discount;
        private final String description;

        public PromotionResult(BigDecimal discount, String description) {
            this.discount = discount != null ? discount : BigDecimal.ZERO;
            this.description = description != null ? description : "";
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("PromotionResult{discount=%s, description='%s'}", discount, description);
        }
    }
}

