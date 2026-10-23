package pt.psoft.g1.psoftg1.lendingmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.psoft.g1.psoftg1.exceptions.GlobalExceptionHandler;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box test of the REST contract of {@link LendingController}: status codes, headers and delegation.
 * Service, user lookup and mapper are doubles, so no Spring context or database is needed (CON-TST-10).
 */
class LendingControllerTest {
    private static final String LENDING_NUMBER = "2026/1";
    private static final String READER_NUMBER = "2026/7";

    private LendingService lendingService;
    private ReaderService readerService;
    private UserService userService;
    private LendingViewMapper mapper;
    private MockMvc mockMvc;

    private Authentication authentication;
    private User loggedUser;
    private Lending lending;

    @BeforeEach
    void setUp() {
        lendingService = mock(LendingService.class);
        readerService = mock(ReaderService.class);
        userService = mock(UserService.class);
        mapper = mock(LendingViewMapper.class);
        authentication = mock(Authentication.class);

        final var controller = new LendingController(lendingService, readerService, userService,
                new ConcurrencyService(), mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        final var owner = mock(ReaderDetails.class);
        when(owner.getReaderNumber()).thenReturn(READER_NUMBER);
        lending = mock(Lending.class);
        when(lending.getLendingNumber()).thenReturn(LENDING_NUMBER);
        when(lending.getVersion()).thenReturn(3L);
        when(lending.getReaderDetails()).thenReturn(owner);
        when(lendingService.findByLendingNumber(LENDING_NUMBER)).thenReturn(Optional.of(lending));

        final var view = new LendingView();
        view.setLendingNumber(LENDING_NUMBER);
        when(mapper.toLendingView(any(Lending.class))).thenReturn(view);

        loggedUser = mock(User.class);
        when(loggedUser.getUsername()).thenReturn("reader@mail.com");
        when(userService.getAuthenticatedUser(authentication)).thenReturn(loggedUser);
    }

    private void loggedReaderHasNumber(String readerNumber) {
        final var details = mock(ReaderDetails.class);
        when(details.getReaderNumber()).thenReturn(readerNumber);
        when(readerService.findByUsername("reader@mail.com")).thenReturn(Optional.of(details));
    }

    @Test
    void createReturns201WithLocationAndEtag() throws Exception {
        when(lendingService.create(any(CreateLendingRequest.class))).thenReturn(lending);

        mockMvc.perform(post("/api/lendings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9782826012092\",\"readerNumber\":\"" + READER_NUMBER + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/lendings/" + LENDING_NUMBER)))
                .andExpect(jsonPath("$.lendingNumber").value(LENDING_NUMBER));

        final var captor = ArgumentCaptor.forClass(CreateLendingRequest.class);
        verify(lendingService).create(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("9782826012092", captor.getValue().getIsbn());
        org.junit.jupiter.api.Assertions.assertEquals(READER_NUMBER, captor.getValue().getReaderNumber());
    }

    @Test
    void createRejectedByPolicyReturns403WithTheReason() throws Exception {
        when(lendingService.create(any(CreateLendingRequest.class)))
                .thenThrow(new LendingForbiddenException("Reader has book(s) past their due date"));

        mockMvc.perform(post("/api/lendings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"9782826012092\",\"readerNumber\":\"" + READER_NUMBER + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reader has book(s) past their due date")));
    }

    @Test
    void createWithInvalidBodyReturns400AndDoesNotCallTheService() throws Exception {
        mockMvc.perform(post("/api/lendings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isbn\":\"123\",\"readerNumber\":\"" + READER_NUMBER + "\"}"))
                .andExpect(status().isBadRequest());

        verify(lendingService, never()).create(any());
    }

    @Test
    void findByLendingNumberAsLibrarianReturns200() throws Exception {
        final var librarian = mock(Librarian.class);
        when(userService.getAuthenticatedUser(authentication)).thenReturn(librarian);

        mockMvc.perform(get("/api/lendings/2026/1").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.lendingNumber").value(LENDING_NUMBER));
    }

    @Test
    void findByLendingNumberAsOwnerReturns200() throws Exception {
        loggedReaderHasNumber(READER_NUMBER);

        mockMvc.perform(get("/api/lendings/2026/1").principal(authentication))
                .andExpect(status().isOk());
    }

    @Test
    void findByLendingNumberAsAnotherReaderReturns403() throws Exception {
        loggedReaderHasNumber("2026/99");

        mockMvc.perform(get("/api/lendings/2026/1").principal(authentication))
                .andExpect(status().isForbidden());
    }

    @Test
    void findUnknownLendingReturns404() throws Exception {
        when(lendingService.findByLendingNumber("2026/55")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/lendings/2026/55").principal(authentication))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnWithoutIfMatchReturns400() throws Exception {
        mockMvc.perform(patch("/api/lendings/2026/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentary\":\"ok\"}"))
                .andExpect(status().isBadRequest());

        verify(lendingService, never()).setReturned(any(), any(), anyLong());
    }

    @Test
    void returnByAnotherReaderReturns403() throws Exception {
        loggedReaderHasNumber("2026/99");

        mockMvc.perform(patch("/api/lendings/2026/1")
                        .principal(authentication)
                        .header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentary\":\"ok\"}"))
                .andExpect(status().isForbidden());

        verify(lendingService, never()).setReturned(any(), any(), anyLong());
    }

    @Test
    void returnByOwnerCallsTheServiceWithTheVersionOfTheHeader() throws Exception {
        loggedReaderHasNumber(READER_NUMBER);
        when(lendingService.setReturned(eq(LENDING_NUMBER), any(), eq(3L))).thenReturn(lending);

        mockMvc.perform(patch("/api/lendings/2026/1")
                        .principal(authentication)
                        .header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commentary\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""));
    }

    @Test
    void renewWithoutIfMatchReturns400() throws Exception {
        mockMvc.perform(post("/api/lendings/2026/1/renewal").principal(authentication))
                .andExpect(status().isBadRequest());

        verify(lendingService, never()).renew(any(), anyLong());
    }

    @Test
    void renewOfAnotherReadersLendingReturns403() throws Exception {
        loggedReaderHasNumber("2026/99");

        mockMvc.perform(post("/api/lendings/2026/1/renewal").principal(authentication).header("If-Match", "\"3\""))
                .andExpect(status().isForbidden());

        verify(lendingService, never()).renew(any(), anyLong());
    }

    @Test
    void renewByOwnerReturns200AndPassesTheVersion() throws Exception {
        loggedReaderHasNumber(READER_NUMBER);
        when(lendingService.renew(LENDING_NUMBER, 3L)).thenReturn(lending);

        mockMvc.perform(post("/api/lendings/2026/1/renewal").principal(authentication).header("If-Match", "\"3\""))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.lendingNumber").value(LENDING_NUMBER));
    }

    @Test
    void renewRejectedByPolicyReturns403WithTheReason() throws Exception {
        loggedReaderHasNumber(READER_NUMBER);
        when(lendingService.renew(LENDING_NUMBER, 3L))
                .thenThrow(new LendingForbiddenException("The maximum number of renewals was reached"));

        mockMvc.perform(post("/api/lendings/2026/1/renewal").principal(authentication).header("If-Match", "\"3\""))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("The maximum number of renewals was reached")));
    }

    @Test
    void renewOfUnknownLendingReturns404() throws Exception {
        when(lendingService.findByLendingNumber("2026/55")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/lendings/2026/55/renewal").principal(authentication).header("If-Match", "\"3\""))
                .andExpect(status().isNotFound());
    }
}
