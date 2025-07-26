package model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Entity
@Getter
@Setter
@Table(name = "budget")
@IdClass(Budget.PK.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Budget extends PanacheEntityBase {
    @Id
    @Column(name = "user_sub", length = 36)
    private String userSub;

    @Id
    @ManyToOne
    @JoinColumn(name = "category_id",
            foreignKey = @ForeignKey(name = "fk_budget_category"),
            insertable = false, updatable = false)
    private Category category;

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
