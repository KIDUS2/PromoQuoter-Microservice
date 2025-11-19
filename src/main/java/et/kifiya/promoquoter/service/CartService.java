package et.kifiya.promoquoter.service;


import et.kifiya.promoquoter.dto.CartRequest;
import et.kifiya.promoquoter.dto.ResponseDTO.AppliedPromotion;
import et.kifiya.promoquoter.dto.ResponseDTO.CartConfirmResponse;
import et.kifiya.promoquoter.dto.ResponseDTO.CartItemDto;
import et.kifiya.promoquoter.dto.ResponseDTO.CartResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.CartConfirmRequest;
import et.kifiya.promoquoter.dto.requestDTO.CartItemRequest;
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
import java.time.LocalDateTime;
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
        log.info("=== CALCULATE QUOTE STARTED ===");

        // Validate and get products
        Map<String, Product> products = getProductsFromRequest(request);
        log.info("Products found: {}", products.keySet());

        Map<String, Integer> cartItems = convertToCartItems(request);
        log.info("Cart items: {}", cartItems);

        // Calculate subtotal
        BigDecimal subtotal = calculateSubtotal(products, cartItems);
        log.info("Subtotal: {}", subtotal);

        // Apply promotions
        List<Promotion> activePromotions = promotionService.getActivePromotions();
        log.info("Active promotions found: {}", activePromotions.size());
        activePromotions.forEach(promo -> log.info("Promotion: {} - {} - Active: {}",
                promo.getName(), promo.getType(), promo.isActive()));

        PromotionService.PromotionContext context =
                promotionService.applyPromotions(activePromotions, products, cartItems);
        log.info("Total discount calculated: {}", context.getTotalDiscount());
        log.info("Applied promotions count: {}", context.getAppliedPromotions().size());

        // Build response
        CartResponseDto response = buildQuoteResponse(products, cartItems, subtotal, context);
        log.info("=== CALCULATE QUOTE COMPLETED ===");

        return response;
    }
    @Transactional
    public CartConfirmResponse confirmCart(CartConfirmRequest request, String idempotencyKey) {
        log.info("=== CART CONFIRMATION STARTED ===");
        log.info("Idempotency Key: {}", idempotencyKey);
        log.info("Cart items: {}", request.getItems());

        // Check idempotency first
        if (idempotencyKey != null) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                log.info("Found existing order with idempotency key: {}", idempotencyKey);
                return getOrderConfirmationResponse(existingOrder.get().getId());
            }
        }

        try {
            // Validate stock availability with retry logic for optimistic locking
            return executeWithRetry(() -> processCartConfirmation(request, idempotencyKey));
        } catch (ObjectOptimisticLockingFailureException e) {
            log.error("Optimistic locking failure: {}", e.getMessage());
            throw new IllegalStateException("Unable to process order due to concurrent modification. Please try again.");
        } catch (Exception e) {
            log.error("Error processing cart confirmation: {}", e.getMessage(), e);
            throw new RuntimeException("Error processing cart confirmation: " + e.getMessage(), e);
        }
    }

    private CartConfirmResponse processCartConfirmation(CartConfirmRequest request, String idempotencyKey) {
        log.info("Processing cart confirmation...");

        // Validate stock availability
        validateStockForAllItems(request.getItems());
        log.info("Stock validation passed");

        // Calculate quote
        CartRequest quoteRequest = new CartRequest();
        quoteRequest.setItems(request.getItems());
        quoteRequest.setCustomerSegment(request.getCustomerSegment());
        CartResponseDto quote = calculateQuote(quoteRequest);
        log.info("Quote calculated successfully");

        // Reserve inventory and create order
        Order order = createOrder(request, quote, idempotencyKey);
        log.info("Order created successfully with ID: {}", order.getId());

        return buildConfirmResponse(order, quote);
    }

    private void validateStockForAllItems(List<CartItemRequest> items) {
        log.info("Validating stock for {} items", items.size());
        for (CartItemRequest item : items) {
            log.info("Validating product: {}, quantity: {}", item.getProductId(), item.getQty());
            productService.validateStockAvailability(
                    UUID.fromString(item.getProductId()),
                    item.getQty()
            );
        }
    }

    private CartConfirmResponse processCartConfirmationss(CartConfirmRequest request, String idempotencyKey) {
        // Validate stock availability
        validateStockForAllItems(request.getItems());

        // Calculate quote
        CartRequest quoteRequest = new CartRequest();
        quoteRequest.setItems(request.getItems());
        quoteRequest.setCustomerSegment(request.getCustomerSegment());
        CartResponseDto quote = calculateQuote(quoteRequest);

        // Reserve inventory and create order
        Order order = createOrder(request, quote, idempotencyKey);

        return buildConfirmResponse(order, quote);
    }

    private <T> T executeWithRetry(Callable<T> task) {
        int retryCount = 0;
        int maxRetries = 3;

        while (retryCount < maxRetries) {
            try {
                return task.call();
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e;
                }
                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error processing cart confirmation", e);
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

    private void validateStockForAllItemss(List<CartItemRequest> items) {
        for (CartItemRequest item : items) {
            productService.validateStockAvailability(
                    UUID.fromString(item.getProductId()),
                    item.getQty()
            );
        }
    }

    private Order createOrder(CartConfirmRequest request, CartResponseDto quote, String idempotencyKey) {
        log.info("Creating order with idempotency key: {}", idempotencyKey);

        // Create order first without items
        Order order = new Order();
        // Don't set ID manually - let the constructor handle it
        order.setCustomerSegment(request.getCustomerSegment().name());
        order.setSubtotal(quote.getSubtotal());
        order.setTotalDiscount(quote.getTotalDiscount());
        order.setTotal(quote.getTotal());
        order.setIdempotencyKey(idempotencyKey);

        log.info("Order created with ID: {}", order.getId());

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItemRequest item : request.getItems()) {
            Product product = productService.getProductEntity(UUID.fromString(item.getProductId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(UUID.fromString(item.getProductId()));
            orderItem.setQuantity(item.getQty());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQty())));

            orderItems.add(orderItem);

            // Update stock - use a separate transactional method
            updateProductStock(product.getId(), -item.getQty());
        }

        order.setItems(orderItems);

        // Save the order
        Order savedOrder = orderRepository.save(order);
        log.info("Order saved successfully: {}", savedOrder.getId());

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
        // This will be automatically saved due to @Transactional
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

            // Calculate line item discount (simplified - would track per line in real implementation)
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
        // Simplified implementation - in real scenario, track discounts per line item
        // This would require more sophisticated discount allocation logic
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

    private CartConfirmResponse buildConfirmResponse(Order order, CartResponseDto quote) {
        // Fix: Create CartItemDto objects instead of OrderItem entities for the response
        List<CartItemDto> orderItems = order.getItems().stream()
                .map(item -> {
                    // Fetch product details for the response
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
                            BigDecimal.ZERO, // You might want to calculate actual discount per item
                            lineTotal // finalPrice would be lineTotal - discount
                    );
                })
                .collect(Collectors.toList());

        List<AppliedPromotion> appliedPromotions =
                quote != null ? quote.getAppliedPromotions().stream()
                        .map(ap -> new AppliedPromotion(
                                ap.getPromotionId(),
                                ap.getPromotionName(),
                                ap.getType(), // Make sure this field exists in AppliedPromotion
                                ap.getDescription(),
                                ap.getDiscountAmount(),
                                ap.getOrder() // Make sure this field exists
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
                appliedPromotions
        );
    }

    private CartConfirmResponse getOrderConfirmationResponse(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // For existing orders, we need to reconstruct the quote or get it from order details
        CartResponseDto reconstructedQuote = reconstructQuoteFromOrder(order);

        return buildConfirmResponse(order, reconstructedQuote);
    }

    private CartResponseDto reconstructQuoteFromOrder(Order order) {
        // Reconstruct the quote response from order data
        // This is a simplified implementation - you might want to store quote details in the order

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
                            BigDecimal.ZERO, // Discount would need to be stored in order
                            item.getLineTotal()
                    );
                })
                .collect(Collectors.toList());

        // Applied promotions would need to be stored in the order entity
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
