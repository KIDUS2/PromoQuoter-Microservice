package et.kifiya.promoquoter.UnitTestServices;

import et.kifiya.promoquoter.dto.CartRequest;
import et.kifiya.promoquoter.dto.ApiResponse;
import et.kifiya.promoquoter.dto.ResponseDTO.CartResponseDto;
import et.kifiya.promoquoter.dto.requestDTO.CartItemRequest;
import et.kifiya.promoquoter.enums.Category;
import et.kifiya.promoquoter.enums.CustomerSegment;
import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.model.Promotion;
import et.kifiya.promoquoter.repository.ProductRepository;
import et.kifiya.promoquoter.repository.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CartIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    private Product testProduct;
    private Promotion testPromotion;

    @BeforeEach
    void setUp() {
        // Clean up and create test data
        promotionRepository.deleteAll();
        productRepository.deleteAll();

        testProduct = new Product();
        testProduct.setName("Integration Test Product");
        testProduct.setCategory(Category.ELECTRONICS);
        testProduct.setPrice(new BigDecimal("200.00"));
        testProduct.setStock(10);
        testProduct = productRepository.save(testProduct);

        testPromotion = new Promotion();
        testPromotion.setName("Integration Test Promotion");
        testPromotion.setType(PromotionType.PERCENT_OFF_CATEGORY);
        testPromotion.setCategory("ELECTRONICS");
        testPromotion.setDiscountPercent(new BigDecimal("15.0"));
        testPromotion.setPriority(1);
        testPromotion.setActive(true);
        testPromotion = promotionRepository.save(testPromotion);
    }

    @Test
    void fullCartFlow_ShouldWorkCorrectly() {
        // Arrange
        CartRequest quoteRequest = new CartRequest();
        CartItemRequest item = new CartItemRequest();
        item.setProductId(testProduct.getId().toString());
        item.setQty(2);
        quoteRequest.setItems(List.of(item));
        quoteRequest.setCustomerSegment(CustomerSegment.REGULAR);
        ResponseEntity<ApiResponse> quoteResponse = restTemplate.postForEntity(
                "/api/v1/cart/quote", quoteRequest, ApiResponse.class);
        assertEquals(HttpStatus.OK, quoteResponse.getStatusCode());
        assertNotNull(quoteResponse.getBody());
        assertEquals("SUCCESS", quoteResponse.getBody().getStatus());
        String idempotencyKey = "integration-test-" + System.currentTimeMillis();
        ResponseEntity<ApiResponse> confirmResponse = restTemplate.postForEntity(
                "/api/v1/cart/confirm", quoteRequest, ApiResponse.class);
        assertEquals(HttpStatus.OK, confirmResponse.getStatusCode());
        assertNotNull(confirmResponse.getBody());
        assertEquals("SUCCESS", confirmResponse.getBody().getStatus());
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertEquals(8, updatedProduct.getStock());
    }

    @Test
    void cartQuote_WithInvalidProduct_ShouldReturnError() {
        CartRequest request = new CartRequest();
        CartItemRequest item = new CartItemRequest();
        item.setProductId("invalid-uuid");
        item.setQty(1);
        request.setItems(List.of(item));
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                "/api/v1/cart/quote", request, ApiResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ERROR", response.getBody().getStatus());
    }
}
