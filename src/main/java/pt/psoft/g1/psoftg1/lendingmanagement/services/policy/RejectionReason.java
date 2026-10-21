package pt.psoft.g1.psoftg1.lendingmanagement.services.policy;

/** Why a policy rejected a request; the message is what the reader is told (US13, US14). */
public enum RejectionReason {
    OVERDUE_LENDING("Reader has book(s) past their due date"),
    MAX_ACTIVE_LENDINGS("Reader has reached the maximum number of active lendings"),
    LENDING_OVERDUE("An overdue lending cannot be renewed"),
    MAX_RENEWALS("The maximum number of renewals was reached");

    private final String message;

    RejectionReason(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
