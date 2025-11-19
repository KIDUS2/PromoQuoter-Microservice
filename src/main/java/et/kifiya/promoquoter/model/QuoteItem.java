package et.kifiya.promoquoter.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class QuoteItem {

    private UUID productId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal finalPrice;

    public QuoteItem(UUID productId, String name, int quantity, BigDecimal unitPrice, BigDecimal discount) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount;
        this.finalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount);
    }
}
