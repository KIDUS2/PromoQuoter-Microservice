package et.kifiya.promoquoter.dto.ResponseDTO;

import et.kifiya.promoquoter.enums.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private Category category;
    private BigDecimal price;
    private Integer stock;
}
