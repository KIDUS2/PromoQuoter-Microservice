package et.kifiya.promoquoter.service;


import et.kifiya.promoquoter.dto.ResponseDTO.PromotionResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.PromotionRequestDto;
import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.promotion.PromotionStrategy;
import et.kifiya.promoquoter.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final List<PromotionStrategy> strategies;

    public List<PromotionResponseDto> createPromotions(List<PromotionRequestDto> requests) {
        List<Promotion> promotions = requests.stream()
                .map(this::mapToPromotion)
                .collect(Collectors.toList());

        List<Promotion> savedPromotions = promotionRepository.saveAll(promotions);

        return savedPromotions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionResponseDto> getAllPromotions() {
        return promotionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromotionResponseDto getPromotion(UUID id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promotion not found with id: " + id));
        return mapToResponse(promotion);
    }

    @Transactional(readOnly = true)
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByActiveTrueOrderByPriority();
    }

    @Transactional(readOnly = true)
    public List<Promotion> getPromotionsByType(PromotionType type) {
        return promotionRepository.findByTypeAndActiveTrue(type);
    }

    public PromotionContext applyPromotions(List<Promotion> promotions,
                                            Map<String, Product> products,
                                            Map<String, Integer> cartItems) {
        strategies.forEach(strategy -> log.info("Strategy: {}", strategy.getClass().getSimpleName()));

        PromotionContext context = new PromotionContext(products, cartItems);

        List<Promotion> sortedPromotions = promotions.stream()
                .filter(Promotion::isActive)
                .sorted(Comparator.comparing(Promotion::getPriority))
                .collect(Collectors.toList());

        for (Promotion promotion : sortedPromotions) {
            boolean strategyFound = false;
            for (PromotionStrategy strategy : strategies) {
                if (strategy.supports(promotion)) {
                    strategyFound = true;

                    PromotionStrategy.PromotionResult result =
                            strategy.apply(promotion, context.getProducts(), context.getCartItems());


                    if (result.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                        context.addAppliedPromotion(promotion, result);
                    }
                    break;
                }
            }

            if (!strategyFound) {
                log.warn("No strategy found for promotion: {} (Type: {})",
                        promotion.getName(), promotion.getType());
            }
        }
        return context;
    }

    private Promotion mapToPromotion(PromotionRequestDto request) {
        Promotion promotion = new Promotion();
        promotion.setName(request.getName());
        promotion.setType(request.getType());
        promotion.setCategory(request.getCategory());
        promotion.setDiscountPercent(request.getDiscountPercent());
        promotion.setProductId(request.getProductId());
        promotion.setBuyQuantity(request.getBuyQuantity());
        promotion.setGetQuantity(request.getGetQuantity());
        promotion.setPriority(request.getPriority());
        promotion.setActive(request.getActive() != null ? request.getActive() : true);
        return promotion;
    }

    private PromotionResponseDto mapToResponse(Promotion promotion) {
        return new PromotionResponseDto(
                promotion.getId(),
                promotion.getName(),
                promotion.getType(),
                promotion.getCategory(),
                promotion.getDiscountPercent(),
                promotion.getProductId(),
                promotion.getBuyQuantity(),
                promotion.getGetQuantity(),
                promotion.getPriority(),
                promotion.isActive()
        );
    }

    public static class PromotionContext {
        private final Map<String, Product> products;
        private final Map<String, Integer> cartItems;
        private BigDecimal totalDiscount = BigDecimal.ZERO;
        private final List<AppliedPromotion> appliedPromotions = new java.util.ArrayList<>();

        public PromotionContext(Map<String, Product> products,
                                Map<String, Integer> cartItems) {
            this.products = products;
            this.cartItems = cartItems;
        }

        public void addAppliedPromotion(Promotion promotion, PromotionStrategy.PromotionResult result) {
            totalDiscount = totalDiscount.add(result.getDiscount());
            appliedPromotions.add(new AppliedPromotion(promotion, result));
        }

        public Map<String, Product> getProducts() {
            return products;
        }

        public Map<String, Integer> getCartItems() {
            return cartItems;
        }

        public BigDecimal getTotalDiscount() {
            return totalDiscount;
        }

        public List<AppliedPromotion> getAppliedPromotions() {
            return appliedPromotions;
        }
    }

    public static class AppliedPromotion {
        private final Promotion promotion;
        private final PromotionStrategy.PromotionResult result;

        public AppliedPromotion(Promotion promotion, PromotionStrategy.PromotionResult result) {
            this.promotion = promotion;
            this.result = result;
        }

        public Promotion getPromotion() {
            return promotion;
        }

        public PromotionStrategy.PromotionResult getResult() {
            return result;
        }
    }
}