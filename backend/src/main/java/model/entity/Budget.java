package model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "budget")
@IdClass(Budget.PK.class)
public class Budget extends PanacheEntityBase {
    @Id
    @Column(name = "user_sub", length = 36)
    private String userSub;

    @Id @Column(name="category_id")
    private Long categoryId;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PK implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        public String userSub;
        public Long categoryId;
    }
}
