package et.kifiya.promoquoter.service;


import et.kifiya.promoquoter.dto.CartRequest;
import et.kifiya.promoquoter.dto.ResponseDTO.AppliedPromotion;
import et.kifiya.promoquoter.dto.ResponseDTO.CartConfirmResponse;
import et.kifiya.promoquoter.dto.ResponseDTO.CartItemDto;
import et.kifiya.promoquoter.dto.ResponseDTO.CartResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.CartConfirmRequest;
import et.kifiya.promoquoter.dto.requestDTO.CartItemRequest;
import et.kifiya.promoquoter.exception.OutOfStockException;
import et.kifiya.promoquoter.model.Order;
import et.kifiya.promoquoter.model.OrderItem;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {
    private final ProductService productService;
    private final PromotionService promotionService;
    private final OrderRepository orderRepository;
    private final IdempotencyService idempotencyService;

    @Transactional(readOnly = true)
    public CartResponseDto calculateQuote(CartRequest request) {
        Map<String, Product> products = getProductsFromRequest(request);
        Map<String, Integer> cartItems = convertToCartItems(request);
        BigDecimal subtotal = calculateSubtotal(products, cartItems);
        List<Promotion> activePromotions = promotionService.getActivePromotions();
        activePromotions.forEach(promo -> log.info("Promotion: {} - {} - Active: {}",
                promo.getName(), promo.getType(), promo.isActive()));

        PromotionService.PromotionContext context =
                promotionService.applyPromotions(activePromotions, products, cartItems);
        CartResponseDto response = buildQuoteResponse(products, cartItems, subtotal, context);
        return response;
    }

    @Transactional
    public CartConfirmResponse confirmCart(CartConfirmRequest request, String idempotencyKey) {
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            return getOrderConfirmationResponse(existingOrder.get().getId());
        }

        try {
            return executeWithRetry(() -> processCartConfirmation(request, idempotencyKey));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to process order due to concurrent modification. Please try again.");
        } catch (OutOfStockException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient stock")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing order");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing cart confirmation");
        }
    }

    private CartConfirmResponse processCartConfirmation(CartConfirmRequest request, String idempotencyKey) {
        validateStockForAllItems(request.getItems());
        CartRequest quoteRequest = new CartRequest();
        quoteRequest.setItems(request.getItems());
        quoteRequest.setCustomerSegment(request.getCustomerSegment());
        CartResponseDto quote = calculateQuote(quoteRequest);
        Optional<Order> lastMinuteCheck = orderRepository.findWithLockingByIdempotencyKey(idempotencyKey);
        if (lastMinuteCheck.isPresent()) {
            return getOrderConfirmationResponse(lastMinuteCheck.get().getId());
        }

        Order order = createOrder(request, quote, idempotencyKey);
        return buildConfirmResponse(order, quote, "NEW");
    }

    private void validateStockForAllItems(List<CartItemRequest> items) {
        for (CartItemRequest item : items) {
            productService.validateStockAvailability(
                    UUID.fromString(item.getProductId()),
                    item.getQty()
            );
        }
    }


    private <T> T executeWithRetry(Callable<T> task) {
        int retryCount = 0;
        int maxRetries = 3;

        while (retryCount < maxRetries) {
            try {
                return task.call();
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                log.warn("Optimistic lock failure - attempt {}/{}", retryCount, maxRetries);

                if (retryCount >= maxRetries) {
                    throw e;
                }

                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }

    private Map<String, Product> getProductsFromRequest(CartRequest request) {
        List<UUID> productIds = request.getItems().stream()
                .map(item -> UUID.fromString(item.getProductId()))
                .collect(Collectors.toList());

        List<Product> products = productService.getProductsByIds(productIds);

        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("One or more products not found");
        }

        return products.stream()
                .collect(Collectors.toMap(p -> p.getId().toString(), p -> p));
    }

    private Map<String, Integer> convertToCartItems(CartRequest request) {
        return request.getItems().stream()
                .collect(Collectors.toMap(
                        CartItemRequest::getProductId,
                        CartItemRequest::getQty
                ));
    }

    private BigDecimal calculateSubtotal(Map<String, Product> products, Map<String, Integer> cartItems) {
        return cartItems.entrySet().stream()
                .map(entry -> {
                    Product product = products.get(entry.getKey());
                    return product.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Order createOrder(CartConfirmRequest request, CartResponseDto quote, String idempotencyKey) {
        Order order = new Order();
        order.setCustomerSegment(request.getCustomerSegment().name());
        order.setSubtotal(quote.getSubtotal());
        order.setTotalDiscount(quote.getTotalDiscount());
        order.setTotal(quote.getTotal());
        order.setIdempotencyKey(idempotencyKey);
        List<OrderItem> orderItems = request.getItems().stream()
                .map(item -> {
                    Product product = productService.getProductEntity(UUID.fromString(item.getProductId()));

                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(UUID.fromString(item.getProductId()));
                    orderItem.setQuantity(item.getQty());
                    orderItem.setUnitPrice(product.getPrice());
                    orderItem.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQty())));
                    return orderItem;
                })
                .collect(Collectors.toList());

        order.setItems(orderItems);
        Order savedOrder = orderRepository.save(order);
        for (CartItemRequest item : request.getItems()) {
            updateProductStock(UUID.fromString(item.getProductId()), -item.getQty());
        }

        return savedOrder;
    }

    @Transactional
    public void updateProductStock(UUID productId, Integer quantityChange) {
        Product product = productService.getProductEntity(productId);
        int newStock = product.getStock() + quantityChange;

        if (newStock < 0) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }

        product.setStock(newStock);
    }

    private CartResponseDto buildQuoteResponse(Map<String, Product> products,
                                               Map<String, Integer> cartItems,
                                               BigDecimal subtotal,
                                               PromotionService.PromotionContext context) {
        List<CartItemDto> lineItems = buildLineItems(products, cartItems, context);
        List<AppliedPromotion> appliedPromotions = buildAppliedPromotions(context);

        BigDecimal totalDiscount = context.getTotalDiscount();
        BigDecimal total = subtotal.subtract(totalDiscount).max(BigDecimal.ZERO);
        String quoteId = UUID.randomUUID().toString();

        return new CartResponseDto(lineItems, appliedPromotions, subtotal, totalDiscount, total, quoteId);
    }

    private List<CartItemDto> buildLineItems(Map<String, Product> products,
                                             Map<String, Integer> cartItems,
                                             PromotionService.PromotionContext context) {
        List<CartItemDto> lineItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
            String productId = entry.getKey();
            Product product = products.get(productId);
            Integer quantity = entry.getValue();
            BigDecimal unitPrice = product.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            BigDecimal lineDiscount = calculateLineItemDiscount(productId, quantity, context);
            BigDecimal finalPrice = lineTotal.subtract(lineDiscount).max(BigDecimal.ZERO);

            lineItems.add(new CartItemDto(
                    productId,
                    product.getName(),
                    product.getCategory().name(),
                    quantity,
                    unitPrice,
                    lineTotal,
                    lineDiscount,
                    finalPrice
            ));
        }

        return lineItems;
    }

    private BigDecimal calculateLineItemDiscount(String productId, Integer quantity,
                                                 PromotionService.PromotionContext context) {
        return BigDecimal.ZERO;
    }

    private List<AppliedPromotion> buildAppliedPromotions(PromotionService.PromotionContext context) {
        List<AppliedPromotion> appliedPromotions = new ArrayList<>();
        int order = 1;

        for (PromotionService.AppliedPromotion ap : context.getAppliedPromotions()) {
            appliedPromotions.add(new AppliedPromotion(
                    ap.getPromotion().getId().toString(),
                    ap.getPromotion().getName(),
                    ap.getPromotion().getType().name(),
                    ap.getResult().getDescription(),
                    ap.getResult().getDiscount(),
                    order++
            ));
        }

        return appliedPromotions;
    }

    private CartConfirmResponse buildConfirmResponse(Order order, CartResponseDto quote, String request) {
        List<CartItemDto> orderItems = order.getItems().stream()
                .map(item -> {
                    Product product = productService.getProductEntity(item.getProductId());
                    BigDecimal unitPrice = product.getPrice();
                    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                    return new CartItemDto(
                            item.getProductId().toString(),
                            product.getName(),
                            product.getCategory().name(),
                            item.getQuantity(),
                            unitPrice,
                            lineTotal,
                            BigDecimal.ZERO,
                            lineTotal
                    );
                })
                .collect(Collectors.toList());

        List<AppliedPromotion> appliedPromotions =
                quote != null ? quote.getAppliedPromotions().stream()
                        .map(ap -> new AppliedPromotion(
                                ap.getPromotionId(),
                                ap.getPromotionName(),
                                ap.getType(),
                                ap.getDescription(),
                                ap.getDiscountAmount(),
                                ap.getOrder()
                        ))
                        .collect(Collectors.toList()) : Collections.emptyList();

        return new CartConfirmResponse(
                order.getId(),
                order.getStatus(),
                order.getOrderDate(),
                orderItems,
                order.getSubtotal(),
                order.getTotalDiscount(),
                order.getTotal(),
                appliedPromotions,
                request
        );
    }

    private CartConfirmResponse getOrderConfirmationResponse(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        CartResponseDto reconstructedQuote = reconstructQuoteFromOrder(order);
        return buildConfirmResponse(order, reconstructedQuote, "Duplicated");
    }

    private CartResponseDto reconstructQuoteFromOrder(Order order) {
        List<CartItemDto> lineItems = order.getItems().stream()
                .map(item -> {
                    Product product = productService.getProductEntity(item.getProductId());
                    return new CartItemDto(
                            item.getProductId().toString(),
                            product.getName(),
                            product.getCategory().name(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getLineTotal(),
                            BigDecimal.ZERO,
                            item.getLineTotal()
                    );
                })
                .collect(Collectors.toList());

        List<AppliedPromotion> appliedPromotions = Collections.emptyList();

        return new CartResponseDto(
                lineItems,
                appliedPromotions,
                order.getSubtotal(),
                order.getTotalDiscount(),
                order.getTotal(),
                "reconstructed-" + order.getId().toString()
        );
    }
}
