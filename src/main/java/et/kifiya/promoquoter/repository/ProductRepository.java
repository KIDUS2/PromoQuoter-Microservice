package et.kifiya.promoquoter.repository;

import et.kifiya.promoquoter.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {}