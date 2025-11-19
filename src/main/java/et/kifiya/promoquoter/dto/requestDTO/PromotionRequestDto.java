package et.kifiya.promoquoter.dto.requestDTO;

import et.kifiya.promoquoter.enums.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PromotionRequestDto {
    @NotBlank
    private String name;

    @NotNull
    private PromotionType type;

    private String category;

    @DecimalMin("0.0")
    private BigDecimal discountPercent;

    private UUID productId;

    @Min(1)
    private Integer buyQuantity;

    @Min(1)
    private Integer getQuantity;

    @Min(0)
    private Integer priority = 0;

    private Boolean active = true;
}
