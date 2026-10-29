package pt.psoft.g1.psoftg1.notificationmanagement.services;

/**
 * Setup-time notification parameters, validated on construction so an invalid value stops startup.
 *
 * @param dueDateNoticeDays days before the limit date on which the due-date notification is sent (US6).
 */
public record NotificationParameters(int dueDateNoticeDays) {

    public NotificationParameters {
        if (dueDateNoticeDays <= 0) {
            throw new IllegalArgumentException("DueDateNoticeDays=" + dueDateNoticeDays + " must be > 0");
        }
    }
}
