package pt.psoft.g1.psoftg1.notificationmanagement.services;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Class SUT: EmailNotificationService. ReaderRepository and the SMTP sender are doubles, so no message leaves the
 * test (TST-NOT-02, CON-TST-8). ReaderDetails is a prepared fixture: only its getters run.
 */
class EmailNotificationServiceTest {
    private static final String FROM = "library@example.com";
    private static final String READER_NUMBER = "2026/1";
    private static final LocalDate LIMIT = LocalDate.of(2026, 3, 13);

    private final ReaderRepository readers = mock(ReaderRepository.class);
    private final JavaMailSender smtp = mock(JavaMailSender.class);
    private final EmailNotificationService channel = new EmailNotificationService(readers, smtp, FROM);

    private void readerExists() {
        final ReaderDetails reader = new ReaderDetails(1, Reader.newReader("manuel@example.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
                "2000-01-01", "919191919", true, true, true, null, null);
        when(readers.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.of(reader));
    }

    private SimpleMailMessage sentMessage() {
        final ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(sent.capture());
        return sent.getValue();
    }

    @Test
    void dueDateNoticeIsSentToTheReadersUsernameWithTheLendingAndTheDate() {
        readerExists();

        channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT));

        final SimpleMailMessage mail = sentMessage();
        assertThat(mail.getFrom()).isEqualTo(FROM);
        assertThat(mail.getTo()).containsExactly("manuel@example.com");
        assertThat(mail.getSubject()).isEqualTo("Lending 2026/5 is due on 2026-03-13");
        assertThat(mail.getText()).contains("O Inspetor Max", "2026/5", "must be returned by 2026-03-13");
    }

    @Test
    void overdueNoticeCarriesTheDaysOverdueAndTheFine() {
        readerExists();

        channel.send(READER_NUMBER, LendingNotification.overdue("2026/5", "O Inspetor Max", LIMIT, 5, 1000));

        final SimpleMailMessage mail = sentMessage();
        assertThat(mail.getTo()).containsExactly("manuel@example.com");
        assertThat(mail.getSubject()).isEqualTo("Lending 2026/5 is overdue");
        assertThat(mail.getText()).contains("2026/5", "was due on 2026-03-13", "5 day(s) overdue",
                "A fine of 10.00 € is being applied");
    }

    @Test
    void unknownReaderIsReportedAndNothingIsSent() {
        when(readers.findByReaderNumber(READER_NUMBER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(smtp);
    }

    // The channel does not decide what a failure means; it reports it to the dispatcher.
    @Test
    void smtpFailureIsReportedToTheCaller() {
        readerExists();
        doThrow(new MailSendException("SMTP server unavailable")).when(smtp).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> channel.send(READER_NUMBER, LendingNotification.dueDateApproaching("2026/5", "O Inspetor Max", LIMIT)))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    void identifiesItselfAsTheEmailChannel() {
        assertThat(channel.channel()).isEqualTo("email");
    }
}
