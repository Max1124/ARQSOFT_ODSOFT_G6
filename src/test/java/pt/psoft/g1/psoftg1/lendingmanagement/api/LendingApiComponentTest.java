package pt.psoft.g1.psoftg1.lendingmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box test of controller + service + policies + repositories + database working together (1.3.3.4.4).
 * Only authentication is simulated; policy values come from library.properties (Standard: 3 active, 15 days,
 * 2 renewals of 7 days; Adaptive: 21/15/7 days; selection: 5 completed in 6 months, 90% on time).
 */
class LendingApiComponentTest extends ApiIntegrationTestSupport {
    private static final String LENDINGS = "/api/lendings";

    @Autowired
    private FineRepository fineRepository;

    private final LocalDate today = LocalDate.now();
    private Librarian librarian;
    private ReaderDetails reader;
    private final List<Book> books = new ArrayList<>();

    @BeforeEach
    void setUp() {
        librarian = newLibrarian("maria@example.com");
        reader = newReader("manuel@example.com");
        final var genre = newGenre("Romance");
        final var author = newAuthor("Manuel Antonio Pina");
        for (int i = 0; i < 8; i++) {
            books.add(newBook("Livro " + i, genre, author));
        }
    }

    private String lendingBody(Book book) {
        return "{\"isbn\":\"" + book.getIsbn() + "\",\"readerNumber\":\"" + reader.getReaderNumber() + "\"}";
    }

    private MvcResult lend(Book book) throws Exception {
        return mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(book)))
                .andReturn();
    }

    private String lendingPath(String lendingNumber) {
        return LENDINGS + "/" + lendingNumber;
    }

    /** Current version of a lending, read the way a client would (ETag of a GET). */
    private String etagOf(String lendingNumber) throws Exception {
        return mockMvc.perform(get(lendingPath(lendingNumber)).with(asUser(librarian, Role.LIBRARIAN)))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("ETag");
    }

    private String numberOf(MvcResult created) throws Exception {
        final String body = created.getResponse().getContentAsString();
        return body.replaceAll(".*\"lendingNumber\":\"([^\"]+)\".*", "$1");
    }

    // new lending: Standard policy

    @Test
    void readerWithoutHistoryGetsTheStandardPeriod() throws Exception {
        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(books.get(0))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.bookTitle").value("Livro 0"))
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(15).toString()));

        assertThat(lendingRepository.listOutstandingByReaderNumber(reader.getReaderNumber())).hasSize(1);
    }

    @Test
    void fourthSimultaneousLendingIsRejectedWithTheReason() throws Exception {
        for (int i = 0; i < 3; i++) {
            lend(books.get(i));
        }

        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(books.get(3))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("maximum number of active lendings")));

        assertThat(lendingRepository.listOutstandingByReaderNumber(reader.getReaderNumber())).hasSize(3);
    }

    @Test
    void readerWithAnOverdueLendingIsRejectedWithTheReason() throws Exception {
        seedLending(books.get(0), reader, today.minusDays(30), null, 15);

        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(books.get(1))))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("past their due date")));
    }

    @Test
    void lendingOfAnUnknownBookIsNotFound() throws Exception {
        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"" + nextValidIsbn() + "\",\"readerNumber\":\"" + reader.getReaderNumber() + "\"}"))
                .andExpect(status().isNotFound());
    }

    // new lending: selection of the Adaptive policy

    private void seedCompletedLendings(int total, int late) {
        for (int i = 0; i < total; i++) {
            final LocalDate start = today.minusDays(60 + i);
            final LocalDate returned = i < late ? start.plusDays(15 + 3) : start.plusDays(10);
            seedLending(books.get(i), reader, start, returned, 15);
        }
    }

    @Test
    void reliableReaderIsSelectedForTheAdaptivePolicy() throws Exception {
        seedCompletedLendings(5, 0);

        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(books.get(5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(21).toString()));
    }

    @Test
    void readerBelowTheOnTimeRatioKeepsTheStandardPolicy() throws Exception {
        seedCompletedLendings(5, 1); // 80% on time, below the 90% required

        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(books.get(5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(15).toString()));
    }

    @Test
    void readerWithTooFewCompletedLendingsKeepsTheStandardPolicy() throws Exception {
        seedCompletedLendings(4, 0);

        mockMvc.perform(post(LENDINGS).with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON).content(lendingBody(books.get(5))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(15).toString()));
    }

    @Test
    void adaptivePeriodShrinksAsTheReaderBorrowsMore() throws Exception {
        seedCompletedLendings(5, 0);

        final var first = lend(books.get(5));   // 0 active -> 21 days
        final var second = lend(books.get(6));  // 1 active -> 21 days
        final var third = lend(books.get(7));   // 2 active -> 15 days (first threshold)

        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getContentAsString()).contains(today.plusDays(21).toString());
        assertThat(third.getResponse().getContentAsString()).contains(today.plusDays(15).toString());
    }

    // renewal

    @Test
    void ownerRenewsAndTheLimitDateMovesByTheRenewalPeriod() throws Exception {
        final String number = numberOf(lend(books.get(0)));

        mockMvc.perform(post(lendingPath(number) + "/renewal").with(asUser(reader.getReader(), Role.READER))
                        .header("If-Match", etagOf(number)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limitDate").value(today.plusDays(15 + 7).toString()));

        assertThat(lendingRepository.findByLendingNumber(number).orElseThrow().getRenewals()).isEqualTo(1);
    }

    @Test
    void thirdRenewalIsRejectedByTheStandardPolicy() throws Exception {
        final String number = numberOf(lend(books.get(0)));
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post(lendingPath(number) + "/renewal").with(asUser(reader.getReader(), Role.READER))
                            .header("If-Match", etagOf(number)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post(lendingPath(number) + "/renewal").with(asUser(reader.getReader(), Role.READER))
                        .header("If-Match", etagOf(number)))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("maximum number of renewals")));
    }

    @Test
    void renewalOfAnOverdueLendingIsRejected() throws Exception {
        final var overdue = seedLending(books.get(0), reader, today.minusDays(30), null, 15);

        mockMvc.perform(post(lendingPath(overdue.getLendingNumber()) + "/renewal").with(asUser(reader.getReader(), Role.READER))
                        .header("If-Match", etagOf(overdue.getLendingNumber())))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("overdue lending cannot be renewed")));
    }

    @Test
    void renewalByAnotherReaderIsForbidden() throws Exception {
        final String number = numberOf(lend(books.get(0)));
        final var other = newReader("joao@example.com");

        mockMvc.perform(post(lendingPath(number) + "/renewal").with(asUser(other.getReader(), Role.READER))
                        .header("If-Match", etagOf(number)))
                .andExpect(status().isForbidden());
    }

    @Test
    void renewalWithAStaleVersionIsRejectedAndChangesNothing() throws Exception {
        final String number = numberOf(lend(books.get(0)));

        mockMvc.perform(post(lendingPath(number) + "/renewal").with(asUser(reader.getReader(), Role.READER))
                        .header("If-Match", "\"99\""))
                .andExpect(status().is4xxClientError());

        assertThat(lendingRepository.findByLendingNumber(number).orElseThrow().getRenewals()).isZero();
    }

    // return

    @Test
    void lateReturnCreatesAFineProportionalToTheDelay() throws Exception {
        final var overdue = seedLending(books.get(0), reader, today.minusDays(20), null, 15); // 5 days late

        mockMvc.perform(patch(lendingPath(overdue.getLendingNumber())).with(asUser(reader.getReader(), Role.READER))
                        .header("If-Match", etagOf(overdue.getLendingNumber()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commentary\":\"Desculpem\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnedDate").value(today.toString()))
                .andExpect(jsonPath("$.daysOverdue").value(5));

        final var fine = fineRepository.findByLendingNumber(overdue.getLendingNumber()).orElseThrow();
        assertThat(fine.getCentsValue()).isEqualTo(5 * 200);
    }

    @Test
    void returnOnTimeCreatesNoFine() throws Exception {
        final String number = numberOf(lend(books.get(0)));

        mockMvc.perform(patch(lendingPath(number)).with(asUser(reader.getReader(), Role.READER))
                        .header("If-Match", etagOf(number))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        assertThat(fineRepository.findByLendingNumber(number)).isEmpty();
        assertThat(lendingRepository.listOutstandingByReaderNumber(reader.getReaderNumber())).isEmpty();
    }

    // queries

    @Test
    void searchFindsLendingsByReaderAndReturnedState() throws Exception {
        lend(books.get(0));
        seedLending(books.get(1), reader, today.minusDays(40), today.minusDays(30), 15);

        mockMvc.perform(post(LENDINGS + "/search").with(asUser(librarian, Role.LIBRARIAN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"number\":1,\"limit\":10},\"query\":{\"readerNumber\":\"" + reader.getReaderNumber()
                                + "\",\"returned\":false}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].bookTitle").value("Livro 0"));
    }

    @Test
    void ownerAndLibrarianCanReadALendingButAnotherReaderCannot() throws Exception {
        final String number = numberOf(lend(books.get(0)));
        final var other = newReader("joao@example.com");

        mockMvc.perform(get(lendingPath(number)).with(asUser(reader.getReader(), Role.READER))).andExpect(status().isOk());
        mockMvc.perform(get(lendingPath(number)).with(asUser(librarian, Role.LIBRARIAN))).andExpect(status().isOk());
        mockMvc.perform(get(lendingPath(number)).with(asUser(other.getReader(), Role.READER))).andExpect(status().isForbidden());
    }
}
