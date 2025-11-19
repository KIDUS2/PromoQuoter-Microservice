package et.kifiya.promoquoter.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Quote {

    private List<QuoteItem> items = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;
    private List<String> appliedPromotions = new ArrayList<>();

    public void addItem(QuoteItem item) {
        items.add(item);
        total = total.add(item.getFinalPrice());
    }

    public void addPromotion(String description) {
        appliedPromotions.add(description);
    }
}
