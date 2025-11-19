package et.kifiya.promoquoter.dto.ResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppliedPromotion {

    private String promotionId;
    private String promotionName;
    private String type;
    private String description;
    private BigDecimal discountAmount;
    private Integer order;
}
