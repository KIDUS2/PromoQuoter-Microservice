package et.kifiya.promoquoter.dto;

import et.kifiya.promoquoter.model.QuoteItem;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class QuoteResponse {
    private List<QuoteItem> items;
    private BigDecimal total;
    private List<String> appliedPromotions;
}

