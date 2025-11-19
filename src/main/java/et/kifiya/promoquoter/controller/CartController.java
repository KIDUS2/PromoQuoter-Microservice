package et.kifiya.promoquoter.controller;


import et.kifiya.promoquoter.dto.CartRequest;
import et.kifiya.promoquoter.dto.ResponseDTO.CartConfirmResponse;
import et.kifiya.promoquoter.dto.ResponseDTO.CartResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.CartConfirmRequest;
import et.kifiya.promoquoter.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @PostMapping("/quote")
    public ResponseEntity<CartResponseDto> calculateQuote(@Valid @RequestBody CartRequest request) {
        CartResponseDto response = cartService.calculateQuote(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<CartConfirmResponse> confirmCart(
            @Valid @RequestBody CartConfirmRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        CartConfirmResponse response = cartService.confirmCart(request, idempotencyKey);
        return ResponseEntity.ok(response);
    }
}