package pt.psoft.g1.psoftg1.lendingmanagement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The {@code Fine} class models a fine applied when a lending is past its due date.
 * <p>It stores its current value, and the associated {@code Lending}.
 * @author  rmfranca*/
@Getter
@Entity
public class Fine {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long pk;

    @PositiveOrZero
    @Column(updatable = false)
    private int fineValuePerDayInCents;

    /**Fine value in Euro cents*/
    @PositiveOrZero
    int centsValue;

    @Setter
    @OneToOne(optional = false, orphanRemoval = true)
    @JoinColumn(name = "lending_pk", nullable = false, unique = true)
    private Lending lending;

    /**
     * Constructs a new {@code Fine} object. Sets the current value of the fine,
     * as well as the fine value per day at the time of creation.
     * @param   lending transaction which generates this fine.
     * @param   today reference date used to compute the days of delay.
     * */
    public Fine(Lending lending, LocalDate today) {
        // Delay is measured at the supplied date so a fine can be computed for any day, not only the real today.
        if(lending.getDaysDelayed(today) <= 0)
            throw new IllegalArgumentException("Lending is not overdue");
        fineValuePerDayInCents = lending.getFineValuePerDayInCents();
        centsValue = fineValuePerDayInCents * lending.getDaysDelayed(today);
        this.lending = Objects.requireNonNull(lending);
    }

    /**Protected empty constructor for ORM only.*/
    protected Fine() {}
}
