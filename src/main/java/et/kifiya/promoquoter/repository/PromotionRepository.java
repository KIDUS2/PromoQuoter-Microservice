package et.kifiya.promoquoter.repository;

import et.kifiya.promoquoter.enums.PromotionType;
import et.kifiya.promoquoter.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    List<Promotion> findByActiveTrueOrderByPriority();
    List<Promotion> findByTypeAndActiveTrue(PromotionType type);
}
