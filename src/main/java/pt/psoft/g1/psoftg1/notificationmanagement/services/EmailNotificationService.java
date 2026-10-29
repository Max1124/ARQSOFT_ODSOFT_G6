package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;

/**
 * Email channel (EXT-NOT-02). The address is the username of the reader's {@code Reader} (CON-NOT-4).
 * Delivery is through the SMTP server configured at setup; the mail client stays inside this class (CON-NOT-5).
 */
public class EmailNotificationService implements NotificationService {
    public static final String CHANNEL = "email";

    private final ReaderRepository readerRepository;
    private final JavaMailSender mailSender;
    private final String fromAddress;

    // The sender is received so tests can replace the SMTP server; 0 real messages (TST-NOT-02).
    public EmailNotificationService(ReaderRepository readerRepository, JavaMailSender mailSender, String fromAddress) {
        this.readerRepository = readerRepository;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public String channel() {
        return CHANNEL;
    }

    @Override
    public void send(String readerNumber, LendingNotification notification) {
        final ReaderDetails reader = readerRepository.findByReaderNumber(readerNumber)
                .orElseThrow(() -> new NotFoundException(ReaderDetails.class, readerNumber));

        final SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(reader.getReader().getUsername());
        message.setSubject(subject(notification));
        message.setText(text(notification));
        mailSender.send(message);
    }

    private static String subject(LendingNotification n) {
        if (n.type() == LendingNotification.Type.OVERDUE) {
            return "Lending " + n.lendingNumber() + " is overdue";
        }
        return "Lending " + n.lendingNumber() + " is due on " + n.limitDate();
    }

    private static String text(LendingNotification n) {
        if (n.type() == LendingNotification.Type.OVERDUE) {
            return "The book '" + n.bookTitle() + "' (lending " + n.lendingNumber() + ") was due on " + n.limitDate()
                    + " and is " + n.daysOverdue() + " day(s) overdue. A fine of " + formatEuros(n.fineValueInCents())
                    + " is being applied.";
        }
        return "The book '" + n.bookTitle() + "' (lending " + n.lendingNumber() + ") must be returned by "
                + n.limitDate() + ".";
    }

    private static String formatEuros(long cents) {
        return java.math.BigDecimal.valueOf(cents, 2).toPlainString() + " €";
    }
}
