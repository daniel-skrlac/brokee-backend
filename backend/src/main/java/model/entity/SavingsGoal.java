package model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "savings_goal")
public class SavingsGoal extends PanacheEntityBase {
    @Id
    @Column(name = "user_sub", length = 36)
    private String userSub;

    @Column(name = "target_amt", nullable = false, precision = 18, scale = 2)
    private BigDecimal targetAmt;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;
}
