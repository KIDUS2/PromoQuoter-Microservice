package et.kifiya.promoquoter.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue
    private UUID id;
    private String status;
    private LocalDateTime orderDate;
    private String customerSegment;
    private BigDecimal subtotal;
    private BigDecimal totalDiscount;
    private BigDecimal total;
    @Version
    private Long version;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items;
    @Column(name = "idempotency_key")
    private String idempotencyKey;

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.status = "CONFIRMED";
    }
}
