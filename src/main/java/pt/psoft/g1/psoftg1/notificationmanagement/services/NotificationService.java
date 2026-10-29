package pt.psoft.g1.psoftg1.notificationmanagement.services;

/**
 * Common contract of a notification mechanism (EXT-NOT-01). Each implementation keeps its own protocol,
 * message format and delivery; clients such as {@code LendingServiceImpl} only use this contract (EXT-NOT-03).
 */
public interface NotificationService {

    /** Identifier used in the setup property of enabled channels and in the reader's chosen channels. */
    String channel();

    /**
     * Notifies the reader through this channel. Contact data is read by the implementation itself (EXT-NOT-04).
     *
     * @throws RuntimeException when delivery fails; the caller decides how to continue.
     */
    void send(String readerNumber, LendingNotification notification);
}
