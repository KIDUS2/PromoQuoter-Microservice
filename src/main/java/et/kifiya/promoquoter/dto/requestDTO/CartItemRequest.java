package et.kifiya.promoquoter.dto.requestDTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull(message = "Product ID is required")
    private String productId;

    @NotNull(message = "Quantity is required")
    private Integer qty;
}
