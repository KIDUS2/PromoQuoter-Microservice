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

//    @GetMapping("/category/{category}")
//    public ResponseEntity<List<PromotionResponseDto>> getPromotionsByCategory(@PathVariable String category) {
//        List<PromotionResponseDto> promotions = promotionService.getActivePromotionsByCategory(category);
//        return ResponseEntity.ok(promotions);
//    }
//
//    @GetMapping("/product/{productId}")
//    public ResponseEntity<List<PromotionResponseDto>> getPromotionsForProduct(@PathVariable UUID productId) {
//        List<PromotionResponseDto> promotions = promotionService.getActivePromotionsForProduct(productId);
//        return ResponseEntity.ok(promotions);
//    }
//
//    @PutMapping("/{id}/activate")
//    public ResponseEntity<PromotionResponseDto> activatePromotion(@PathVariable UUID id) {
//        PromotionResponseDto promotion = promotionService.activatePromotion(id);
//        return ResponseEntity.ok(promotion);
//    }
//
//    @PutMapping("/{id}/deactivate")
//    public ResponseEntity<PromotionResponseDto> deactivatePromotion(@PathVariable UUID id) {
//        PromotionResponseDto promotion = promotionService.deactivatePromotion(id);
//        return ResponseEntity.ok(promotion);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deletePromotion(@PathVariable UUID id) {
//        promotionService.deletePromotion(id);
//        return ResponseEntity.noContent().build();
//    }
}