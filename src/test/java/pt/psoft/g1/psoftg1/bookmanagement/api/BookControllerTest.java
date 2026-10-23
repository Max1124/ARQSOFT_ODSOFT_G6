package pt.psoft.g1.psoftg1.bookmanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookCountDTO;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.bookmanagement.services.CreateBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.GlobalExceptionHandler;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box test of the REST contract of {@link BookController}: status codes, headers and delegation.
 * Services and mapper are doubles, so no Spring context or database is needed (CON-TST-10).
 */
class BookControllerTest {
    private static final String ISBN = "9782826012092";

    private BookService bookService;
    private LendingService lendingService;
    private FileStorageService fileStorageService;
    private UserService userService;
    private ReaderService readerService;
    private BookViewMapper mapper;
    private MockMvc mockMvc;

    private final Genre fantasy = new Genre("Fantasia");
    private final Author author = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", null);

    @BeforeEach
    void setUp() {
        bookService = mock(BookService.class);
        lendingService = mock(LendingService.class);
        fileStorageService = mock(FileStorageService.class);
        userService = mock(UserService.class);
        readerService = mock(ReaderService.class);
        mapper = mock(BookViewMapper.class);

        final var controller = new BookController(bookService, lendingService, new ConcurrencyService(),
                fileStorageService, userService, readerService, mapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        final var view = new BookView();
        view.setIsbn(ISBN);
        view.setTitle("Encantos de contar");
        when(mapper.toBookView(any(Book.class))).thenReturn(view);
        when(mapper.toBookView(anyList())).thenReturn(List.of(view));
    }

    private Book book(String isbn, String title, String photoURI) {
        final var book = new Book(isbn, title, "Uma descrição", fantasy, List.of(author), photoURI);
        ReflectionTestUtils.setField(book, "version", 3L);
        return book;
    }

    // create

    @Test
    void createReturns201WithLocationAndEtag() throws Exception {
        when(bookService.create(any(CreateBookRequest.class), eq(ISBN))).thenReturn(book(ISBN, "Encantos de contar", null));

        mockMvc.perform(put("/api/books/" + ISBN)
                        .param("title", "Encantos de contar")
                        .param("genre", "Fantasia")
                        .param("authors", "1", "2"))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(header().string("Location", endsWith(ISBN)))
                .andExpect(jsonPath("$.isbn").value(ISBN));

        final var captor = ArgumentCaptor.forClass(CreateBookRequest.class);
        verify(bookService).create(captor.capture(), eq(ISBN));
        assertEquals("Encantos de contar", captor.getValue().getTitle());
        assertEquals("Fantasia", captor.getValue().getGenre());
        assertEquals(List.of(1L, 2L), captor.getValue().getAuthors());
    }

    @Test
    void createIgnoresAPhotoUriSentByTheClient() throws Exception {
        when(bookService.create(any(CreateBookRequest.class), eq(ISBN))).thenReturn(book(ISBN, "Encantos de contar", null));

        mockMvc.perform(put("/api/books/" + ISBN)
                        .param("title", "Encantos de contar")
                        .param("genre", "Fantasia")
                        .param("authors", "1")
                        .param("photoURI", "../../etc/passwd"))
                .andExpect(status().isCreated());

        final var captor = ArgumentCaptor.forClass(CreateBookRequest.class);
        verify(bookService).create(captor.capture(), eq(ISBN));
        org.junit.jupiter.api.Assertions.assertNull(captor.getValue().getPhotoURI());
    }

    @Test
    void createFailureReturns400() throws Exception {
        when(bookService.create(any(CreateBookRequest.class), eq(ISBN)))
                .thenThrow(new ConflictException("Book with ISBN " + ISBN + " already exists"));

        mockMvc.perform(put("/api/books/" + ISBN)
                        .param("title", "Encantos de contar")
                        .param("genre", "Fantasia")
                        .param("authors", "1"))
                .andExpect(status().isBadRequest());
    }

    // find

    @Test
    void findByIsbnReturns200WithEtag() throws Exception {
        when(bookService.findByIsbn(ISBN)).thenReturn(book(ISBN, "Encantos de contar", null));

        mockMvc.perform(get("/api/books/" + ISBN))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.title").value("Encantos de contar"));
    }

    @Test
    void findByUnknownIsbnReturns404() throws Exception {
        when(bookService.findByIsbn("0000000000")).thenThrow(new NotFoundException("Book not found"));

        mockMvc.perform(get("/api/books/0000000000"))
                .andExpect(status().isNotFound());
    }

    // update

    @Test
    void updateWithoutIfMatchReturns400() throws Exception {
        mockMvc.perform(patch("/api/books/" + ISBN).param("title", "Novo título"))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).update(any(), anyString());
    }

    @Test
    void updatePassesTheVersionOfTheHeaderAndTheIsbnOfThePath() throws Exception {
        when(bookService.update(any(UpdateBookRequest.class), eq("3"))).thenReturn(book(ISBN, "Novo título", null));

        mockMvc.perform(patch("/api/books/" + ISBN)
                        .header("If-Match", "\"3\"")
                        .param("title", "Novo título"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""));

        final var captor = ArgumentCaptor.forClass(UpdateBookRequest.class);
        verify(bookService).update(captor.capture(), eq("3"));
        assertEquals(ISBN, captor.getValue().getIsbn());
        assertEquals("Novo título", captor.getValue().getTitle());
    }

    @Test
    void updateFailureReturns409() throws Exception {
        when(bookService.update(any(UpdateBookRequest.class), anyString())).thenThrow(new NotFoundException("Genre not found"));

        mockMvc.perform(patch("/api/books/" + ISBN).header("If-Match", "\"3\"").param("title", "Novo título"))
                .andExpect(status().isConflict());
    }

    // list and search

    @Test
    void findBooksJoinsTheCriteriaWithOrWithoutDuplicatesSortedByTitle() throws Exception {
        final var alpha = book("9782826012092", "Alfa", null);
        final var beta = book("9789722025065", "Beta", null);
        when(bookService.findByTitle("a")).thenReturn(List.of(beta, alpha));
        when(bookService.findByGenre("Fantasia")).thenReturn(List.of(alpha));

        mockMvc.perform(get("/api/books").param("title", "a").param("genre", "Fantasia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isbn").value(ISBN));

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<Book>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).toBookView(captor.capture());
        assertEquals(List.of(alpha, beta), captor.getValue());
        verify(bookService, never()).findByAuthorName(anyString());
    }

    @Test
    void findBooksWithoutResultsReturns404() throws Exception {
        when(bookService.findByTitle("zzz")).thenReturn(List.of());

        mockMvc.perform(get("/api/books").param("title", "zzz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findBooksWithoutCriteriaReturns404() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchDelegatesPageAndQueryToTheService() throws Exception {
        when(bookService.searchBooks(any(), any())).thenReturn(List.of(book(ISBN, "Encantos de contar", null)));

        mockMvc.perform(post("/api/books/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"number\":2,\"limit\":5},\"query\":{\"title\":\"Enc\",\"genre\":\"\",\"authorName\":\"\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isbn").value(ISBN));

        verify(bookService).searchBooks(new Page(2, 5), new SearchBooksQuery("Enc", "", ""));
    }

    @Test
    void top5ReturnsTheMappedCounts() throws Exception {
        final var dto = new BookCountDTO(book(ISBN, "Encantos de contar", null), 4);
        final var countView = new BookCountView();
        countView.setLendingCount(4L);
        when(bookService.findTop5BooksLent()).thenReturn(List.of(dto));
        when(mapper.toBookCountView(anyList())).thenReturn(List.of(countView));

        mockMvc.perform(get("/api/books/top5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lendingCount").value(4));
    }

    @Test
    void averageLendingDurationCombinesTheBookAndTheLendingAverage() throws Exception {
        final var book = book(ISBN, "Encantos de contar", null);
        when(bookService.findByIsbn(ISBN)).thenReturn(book);
        when(lendingService.getAvgLendingDurationByIsbn(ISBN)).thenReturn(12.5);
        final var view = new BookAverageLendingDurationView();
        view.setAverageLendingDuration(12.5);
        when(mapper.toBookAverageLendingDurationView(book, 12.5)).thenReturn(view);

        mockMvc.perform(get("/api/books/" + ISBN + "/avgDuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageLendingDuration").value(12.5));
    }

    // suggestions

    @Test
    void suggestionsAreBuiltFromTheReaderOfTheLoggedUser() throws Exception {
        final var authentication = mock(Authentication.class);
        final var user = mock(User.class);
        when(user.getUsername()).thenReturn("reader@mail.com");
        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        final var reader = mock(ReaderDetails.class);
        when(reader.getReaderNumber()).thenReturn("2026/1");
        when(readerService.findByUsername("reader@mail.com")).thenReturn(Optional.of(reader));
        when(bookService.getBooksSuggestionsForReader("2026/1")).thenReturn(List.of(book(ISBN, "Encantos de contar", null)));

        mockMvc.perform(get("/api/books/suggestions").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isbn").value(ISBN));
    }

    @Test
    void suggestionsForAUserWithoutReaderDetailsReturn404() throws Exception {
        final var authentication = mock(Authentication.class);
        final var user = mock(User.class);
        when(user.getUsername()).thenReturn("librarian@mail.com");
        when(userService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(readerService.findByUsername("librarian@mail.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/suggestions").principal(authentication))
                .andExpect(status().isNotFound());
    }

    // photo

    @Test
    void getPhotoOfABookWithoutPhotoReturns200WithoutBody() throws Exception {
        when(bookService.findByIsbn(ISBN)).thenReturn(book(ISBN, "Encantos de contar", null));

        mockMvc.perform(get("/api/books/" + ISBN + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void getPhotoReturnsTheFileWithItsImageType() throws Exception {
        final byte[] image = {1, 2, 3};
        when(bookService.findByIsbn(ISBN)).thenReturn(book(ISBN, "Encantos de contar", "cover.png"));
        when(fileStorageService.getFile("cover.png")).thenReturn(image);
        when(fileStorageService.getExtension("cover.png")).thenReturn(Optional.of("png"));

        mockMvc.perform(get("/api/books/" + ISBN + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(image));
    }

    @Test
    void getPhotoWhoseFileIsMissingReturns200WithoutBody() throws Exception {
        when(bookService.findByIsbn(ISBN)).thenReturn(book(ISBN, "Encantos de contar", "cover.jpg"));
        when(fileStorageService.getFile("cover.jpg")).thenReturn(null);
        when(fileStorageService.getExtension("cover.jpg")).thenReturn(Optional.of("jpg"));

        mockMvc.perform(get("/api/books/" + ISBN + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void deletePhotoOfABookWithoutPhotoReturns404() throws Exception {
        when(bookService.findByIsbn(ISBN)).thenReturn(book(ISBN, "Encantos de contar", null));

        mockMvc.perform(delete("/api/books/" + ISBN + "/photo"))
                .andExpect(status().isNotFound());

        verify(bookService, never()).removeBookPhoto(anyString(), anyLong());
    }

    @Test
    void deletePhotoRemovesTheFileAndTheReference() throws Exception {
        when(bookService.findByIsbn(ISBN)).thenReturn(book(ISBN, "Encantos de contar", "cover.png"));

        mockMvc.perform(delete("/api/books/" + ISBN + "/photo"))
                .andExpect(status().isOk());

        verify(fileStorageService).deleteFile("cover.png");
        verify(bookService).removeBookPhoto(ISBN, 3L);
    }
}
