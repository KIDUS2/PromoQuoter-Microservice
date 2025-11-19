package et.kifiya.promoquoter.dto;

import et.kifiya.promoquoter.model.QuoteItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class OrderResponse {
    private UUID orderId;
    private BigDecimal total;
    private List<QuoteItem> items;
    private List<String> appliedPromotions;
    private LocalDateTime createdAt;
}
