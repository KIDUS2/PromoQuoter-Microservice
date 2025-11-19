package et.kifiya.promoquoter.model;

import et.kifiya.promoquoter.enums.PromotionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "promotion")
@Data
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
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

    private boolean active = true;
}
