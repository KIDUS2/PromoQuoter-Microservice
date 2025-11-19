package et.kifiya.promoquoter.dto.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItemDto {
    private String productId;
    private String productName;
    private String category;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private BigDecimal discount;
    private BigDecimal finalPrice;

}
