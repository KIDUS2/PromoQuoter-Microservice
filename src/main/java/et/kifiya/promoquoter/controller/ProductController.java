package et.kifiya.promoquoter.controller;


import et.kifiya.promoquoter.dto.ResponseDTO.ProductResponse;
import et.kifiya.promoquoter.dto.requestDTO.ProductRequestDto;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<List<ProductResponse>> createProducts(
            @Valid @RequestBody List<ProductRequestDto> productRequests) {
        List<ProductResponse> responses = productService.createProducts(productRequests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.getProduct(id);
        return ResponseEntity.ok(product);
    }

//    @GetMapping("/category/{category}")
//    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
//        List<ProductResponse> products = productService.getProductsByCategory(category);
//        return ResponseEntity.ok(products);
//    }
//
//    @GetMapping("/stock/low")
//    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
//            @RequestParam(defaultValue = "10") Integer threshold) {
//        List<ProductResponse> products = productService.getLowStockProducts(threshold);
//        return ResponseEntity.ok(products);
//    }
//
//    @GetMapping("/stock/out")
//    public ResponseEntity<List<ProductResponse>> getOutOfStockProducts() {
//        List<ProductResponse> products = productService.getOutOfStockProducts();
//        return ResponseEntity.ok(products);
//    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<Product> updateProductStock(
            @PathVariable UUID id,
            @RequestParam Integer quantityChange) {
        Product product = productService.updateStock(id, quantityChange);
        return ResponseEntity.ok(product);
    }
}