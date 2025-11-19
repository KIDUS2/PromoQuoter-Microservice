package et.kifiya.promoquoter.controller;


import et.kifiya.promoquoter.dto.ResponseDTO.PromotionResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.PromotionRequestDto;
import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<List<PromotionResponseDto>> createPromotions(
            @Valid @RequestBody List<PromotionRequestDto> promotionRequests) {
        List<PromotionResponseDto> responses = promotionService.createPromotions(promotionRequests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponseDto>> getAllPromotions() {
        List<PromotionResponseDto> promotions = promotionService.getAllPromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Promotion>> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();
        return ResponseEntity.ok(promotions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionResponseDto> getPromotionById(@PathVariable UUID id) {
        PromotionResponseDto promotion = promotionService.getPromotion(id);
        return ResponseEntity.ok(promotion);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Promotion>> getPromotionsByType(@PathVariable PromotionType type) {
        List<Promotion> promotions = promotionService.getPromotionsByType(type);
        return ResponseEntity.ok(promotions);
    }
}