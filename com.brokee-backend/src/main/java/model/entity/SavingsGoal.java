package model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goal")
public class SavingsGoal extends PanacheEntityBase {
    @Id
    @Column(name = "user_sub", length = 36)
    public String userSub;

    @Column(name = "target_amt", nullable = false, precision = 18, scale = 2)
    public BigDecimal targetAmt;

    @Column(name = "target_date", nullable = false)
    public LocalDate targetDate;
}
