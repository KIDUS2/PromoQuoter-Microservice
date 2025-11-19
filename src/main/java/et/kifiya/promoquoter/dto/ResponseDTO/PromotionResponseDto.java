package et.kifiya.promoquoter.dto.ResponseDTO;

import et.kifiya.promoquoter.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionResponseDto {

    private UUID id;
    private String name;
    private PromotionType type;
    private String category;
    private BigDecimal discountPercent;
    private UUID productId;
    private Integer buyQuantity;
    private Integer getQuantity;
    private Integer priority;
    private Boolean active;
}
