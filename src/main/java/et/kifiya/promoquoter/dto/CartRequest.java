package et.kifiya.promoquoter.dto;


import et.kifiya.promoquoter.dto.requestDTO.CartItemRequest;
import et.kifiya.promoquoter.enums.CustomerSegment;
import et.kifiya.promoquoter.model.CartItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class CartRequest {
    @NotEmpty(message = "Cart items cannot be empty")
    @Valid
    private List<CartItemRequest> items;
    private CustomerSegment customerSegment;
}