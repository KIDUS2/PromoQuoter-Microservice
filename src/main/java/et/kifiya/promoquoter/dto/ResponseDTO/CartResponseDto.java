package et.kifiya.promoquoter.dto.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartResponseDto {

    private List<CartItemDto> cartItemDtos;
    private List<AppliedPromotion> appliedPromotions;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal total;
    private String quoteId;
}
