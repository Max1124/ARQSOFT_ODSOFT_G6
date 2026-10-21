package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Read-only facts about a reader that policies and selection rules need (EXT-LP-04).
 * Completed and on-time counts already refer to the history window.
 */
public record ReaderLendingHistory(
        LocalDate referenceDate,
        int activeLendings,
        int overdueLendings,
        int completedInWindow,
        int onTimeInWindow) {

    // Plain data with no repository or clock, so policies are tested by building a snapshot directly (QAS11, CON-LP-4).
    public ReaderLendingHistory {
        Objects.requireNonNull(referenceDate, "referenceDate");
        if (activeLendings < 0 || overdueLendings < 0 || completedInWindow < 0 || onTimeInWindow < 0) {
            throw new IllegalArgumentException("Lending counts cannot be negative");
        }
        if (overdueLendings > activeLendings) {
            throw new IllegalArgumentException("Overdue lendings are active lendings, so they cannot exceed them");
        }
        if (onTimeInWindow > completedInWindow) {
            throw new IllegalArgumentException("On-time lendings are completed lendings, so they cannot exceed them");
        }
    }
}
