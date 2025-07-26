package model.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "planned_tx")
public class PlannedTx extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_sub", nullable = false, length = 36)
    public String userSub;

    @Column(nullable = false, length = 255)
    public String title;

    @Column(nullable = false, precision = 18, scale = 2)
    public BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    public LocalDate dueDate;

    @Column(name = "auto_book", nullable = false)
    public Boolean autoBook;
}
