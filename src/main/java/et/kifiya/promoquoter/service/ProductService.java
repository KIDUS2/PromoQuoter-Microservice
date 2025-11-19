package et.kifiya.promoquoter.service;

import et.kifiya.promoquoter.dto.ResponseDTO.ProductResponse;
import et.kifiya.promoquoter.model.Product;
import et.kifiya.promoquoter.dto.requestDTO.ProductRequestDto;
import et.kifiya.promoquoter.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> createProducts(List<ProductRequestDto> requests) {
        List<Product> products = requests.stream()
                .map(this::mapToProduct)
                .collect(Collectors.toList());

        List<Product> savedProducts = productRepository.saveAll(products);

        return savedProducts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByIds(List<UUID> productIds) {
        return productRepository.findAllById(productIds);
    }

    public Product updateStock(UUID productId, Integer quantityChange) {
        Product product = getProductEntity(productId);
        int newStock = product.getStock() + quantityChange;
        if (newStock < 0) {
            throw new IllegalStateException("Insufficient stock for product: " + productId);
        }
        product.setStock(newStock);
        return productRepository.save(product);
    }

    public void validateStockAvailability(UUID productId, Integer requestedQuantity) {
        Product product = getProductEntity(productId);
        if (product.getStock() < requestedQuantity) {
            throw new IllegalStateException(
                    String.format("Insufficient stock for product %s. Requested: %d, Available: %d",
                            productId, requestedQuantity, product.getStock()));
        }
    }

    private Product mapToProduct(ProductRequestDto request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        return product;
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getPrice(),
                product.getStock()
        );
    }

    @Transactional
    public boolean updateStockWithOptimisticLock(UUID productId, Integer quantityChange) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("Product not found"));

                int newStock = product.getStock() + quantityChange;
                if (newStock < 0) {
                    throw new IllegalStateException("Insufficient stock for product: " + productId);
                }

                product.setStock(newStock);
                productRepository.save(product); // This will increment the version
                return true;

            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new IllegalStateException("Unable to update stock due to concurrent access. Please try again.");
                }
                // Wait and retry
                try {
                    Thread.sleep(100 * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        return false;
    }
}