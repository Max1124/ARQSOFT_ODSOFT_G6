package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.time.LocalDate;

/**
 * Lending event sent to a reader: the due date is approaching (US6), or the lending is overdue and a fine
 * is being applied (US7). Plain data, so channels and their tests do not depend on the {@code Lending} entity.
 */
public record LendingNotification(Type type,
                                  String lendingNumber,
                                  String bookTitle,
                                  LocalDate limitDate,
                                  int daysOverdue,
                                  int fineValueInCents) {

    public enum Type { DUE_DATE_APPROACHING, OVERDUE }

    public static LendingNotification dueDateApproaching(String lendingNumber, String bookTitle, LocalDate limitDate) {
        return new LendingNotification(Type.DUE_DATE_APPROACHING, lendingNumber, bookTitle, limitDate, 0, 0);
    }

    public static LendingNotification overdue(String lendingNumber, String bookTitle, LocalDate limitDate,
                                              int daysOverdue, int fineValueInCents) {
        return new LendingNotification(Type.OVERDUE, lendingNumber, bookTitle, limitDate, daysOverdue, fineValueInCents);
    }
}
