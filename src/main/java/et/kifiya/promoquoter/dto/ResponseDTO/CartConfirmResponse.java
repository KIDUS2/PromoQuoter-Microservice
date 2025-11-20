package et.kifiya.promoquoter.dto.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartConfirmResponse {
    private UUID orderId;
    private String status;
    private LocalDateTime orderDate;
    private List<CartItemDto> items;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal total;
    private List<AppliedPromotion> appliedPromotions;
    private String requestType="NEW";


}
