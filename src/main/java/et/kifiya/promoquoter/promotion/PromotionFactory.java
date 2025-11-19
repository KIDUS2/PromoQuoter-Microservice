package et.kifiya.promoquoter.promotion;

import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Promotion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PromotionFactory {

    private final Map<PromotionType, PromotionStrategy> strategyMap;

//    public PromotionStrategyFactory(List<PromotionStrategy> strategies) {
//        // Create a map of strategy type to strategy instance
//        this.strategyMap = strategies.stream()
//                .collect(Collectors.toMap(
//                        this::getSupportedType,
//                        Function.identity()
//                ));
//    }

    /**
     * Get the strategy that supports the given promotion
     */
    public PromotionStrategy getStrategy(Promotion promotion) {
        return strategyMap.get(promotion.getType());
    }

    /**
     * Get the strategy for a specific promotion type
     */
    public PromotionStrategy getStrategy(PromotionType type) {
        return strategyMap.get(type);
    }

    private PromotionType getSupportedType(PromotionStrategy strategy) {
        // This is a simplified approach - in real implementation,
        // you'd need a way to map strategy to supported type
        if (strategy instanceof PercentOffCategoryStrategy) {
            return PromotionType.PERCENT_OFF_CATEGORY;
        } else if (strategy instanceof BuyXGetYStrategy) {
            return PromotionType.BUY_X_GET_Y;
        }
//        } else if (strategy instanceof TieredBulkDiscountStrategy) {
//            return PromotionType.TIERED_BULK_DISCOUNT;
//        } else if (strategy instanceof ShippingWaiverStrategy) {
//            return PromotionType.SHIPPING_WAIVER;
//        }
        throw new IllegalArgumentException("Unknown strategy type: " + strategy.getClass().getSimpleName());
    }
}
