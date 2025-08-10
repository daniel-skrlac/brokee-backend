package model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tx")
public class Transaction extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_sub", nullable = false, length = 60)
    private String userSub;

    @Column(
            name = "type",
            nullable = false,
            length = 1,
            columnDefinition = "CHAR(1) CHECK (type IN ('E','I'))"
    )
    private String type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(
            name = "tx_time",
            nullable = false,
            columnDefinition = "DATETIME2"
    )
    private OffsetDateTime txTime;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(length = 500)
    private String note;

    @Column(name = "location_name", length = 255)
    private String locationName;
}
