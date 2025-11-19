package et.kifiya.promoquoter.dto.requestDTO;

import et.kifiya.promoquoter.enums.CustomerSegment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CartConfirmRequest {

    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemRequest> items;

    private CustomerSegment customerSegment;
    private String quoteId;
}
