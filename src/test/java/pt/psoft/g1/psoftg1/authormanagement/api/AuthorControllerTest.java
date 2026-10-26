package pt.psoft.g1.psoftg1.authormanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.exceptions.GlobalExceptionHandler;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Opaque-box test of the REST contract of {@link AuthorController}: status codes, headers and delegation.
 * Services, file storage and mappers are doubles, so no Spring context or database is needed (CON-TST-10).
 */
class AuthorControllerTest {
    private static final long NUMBER = 1L;

    private AuthorService authorService;
    private AuthorViewMapper authorViewMapper;
    private FileStorageService fileStorageService;
    private BookViewMapper bookViewMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authorService = mock(AuthorService.class);
        authorViewMapper = mock(AuthorViewMapper.class);
        fileStorageService = mock(FileStorageService.class);
        bookViewMapper = mock(BookViewMapper.class);

        final var controller = new AuthorController(authorService, authorViewMapper, new ConcurrencyService(),
                fileStorageService, bookViewMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(authorViewMapper.toAuthorView(any(Author.class))).thenAnswer(i -> view(i.getArgument(0)));
    }

    private static AuthorView view(Author author) {
        final var view = new AuthorView();
        view.setAuthorNumber(author.getAuthorNumber());
        view.setName(author.getName());
        view.setBio(author.getBio());
        return view;
    }

    private Author author(long number, String name, String photoURI) {
        final var author = new Author(name, "Biografia de " + name + ".", photoURI);
        ReflectionTestUtils.setField(author, "authorNumber", number);
        ReflectionTestUtils.setField(author, "version", 3L);
        return author;
    }

    private Author existing(String photoURI) {
        final var author = author(NUMBER, "João Alberto", photoURI);
        when(authorService.findByAuthorNumber(NUMBER)).thenReturn(Optional.of(author));
        return author;
    }

    // create

    @Test
    void createReturns201WithLocationAndEtag() throws Exception {
        when(authorService.create(any(CreateAuthorRequest.class))).thenReturn(author(NUMBER, "João Alberto", null));

        mockMvc.perform(post("/api/authors").param("name", "João Alberto").param("bio", "Biografia de João Alberto."))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(header().string("Location", endsWith("/api/authors")))
                .andExpect(jsonPath("$.authorNumber").value(NUMBER))
                .andExpect(jsonPath("$.name").value("João Alberto"));

        final var captor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(authorService).create(captor.capture());
        assertEquals("João Alberto", captor.getValue().getName());
        assertEquals("Biografia de João Alberto.", captor.getValue().getBio());
    }

    @Test
    void createIgnoresAPhotoUriSentByTheClient() throws Exception {
        when(authorService.create(any(CreateAuthorRequest.class))).thenReturn(author(NUMBER, "João Alberto", null));

        mockMvc.perform(post("/api/authors").param("name", "João Alberto").param("bio", "Bio.")
                        .param("photoURI", "../../etc/passwd"))
                .andExpect(status().isCreated());

        final var captor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(authorService).create(captor.capture());
        assertNull(captor.getValue().getPhotoURI());
    }

    @Test
    void createWithAnUploadedPhotoPassesTheStoredFileName() throws Exception {
        final var photo = new MockMultipartFile("photo", "me.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2});
        when(fileStorageService.getRequestPhoto(any(MultipartFile.class))).thenReturn("stored.png");
        when(authorService.create(any(CreateAuthorRequest.class))).thenReturn(author(NUMBER, "João Alberto", "stored.png"));

        mockMvc.perform(multipart("/api/authors").file(photo).param("name", "João Alberto").param("bio", "Bio."))
                .andExpect(status().isCreated());

        final var captor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(authorService).create(captor.capture());
        assertEquals("stored.png", captor.getValue().getPhotoURI());
    }

    @Test
    void createWithInvalidDataReturns400() throws Exception {
        when(authorService.create(any(CreateAuthorRequest.class)))
                .thenThrow(new IllegalArgumentException("Name can only contain alphanumeric characters"));

        mockMvc.perform(post("/api/authors").param("name", "João@Alberto").param("bio", "Bio."))
                .andExpect(status().isBadRequest());
    }

    // update

    @Test
    void updateWithoutIfMatchReturns400AndChangesNothing() throws Exception {
        mockMvc.perform(patch("/api/authors/" + NUMBER).param("name", "Maria José"))
                .andExpect(status().isBadRequest());

        verify(authorService, never()).partialUpdate(anyLong(), any(), anyLong());
    }

    @Test
    void updatePassesTheVersionOfTheIfMatchHeader() throws Exception {
        when(authorService.partialUpdate(eq(NUMBER), any(UpdateAuthorRequest.class), eq(3L)))
                .thenReturn(author(NUMBER, "Maria José", null));

        mockMvc.perform(patch("/api/authors/" + NUMBER).header("If-Match", "\"3\"").param("name", "Maria José"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.name").value("Maria José"));

        final var captor = ArgumentCaptor.forClass(UpdateAuthorRequest.class);
        verify(authorService).partialUpdate(eq(NUMBER), captor.capture(), eq(3L));
        assertEquals("Maria José", captor.getValue().getName());
        assertNull(captor.getValue().getBio());
    }

    @Test
    void updateWithAnUploadedPhotoPassesTheStoredFileName() throws Exception {
        final var photo = new MockMultipartFile("photo", "me.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1});
        when(fileStorageService.getRequestPhoto(any(MultipartFile.class))).thenReturn("stored.jpg");
        when(authorService.partialUpdate(eq(NUMBER), any(UpdateAuthorRequest.class), eq(3L)))
                .thenReturn(author(NUMBER, "João Alberto", "stored.jpg"));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/authors/" + NUMBER).file(photo).header("If-Match", "3"))
                .andExpect(status().isOk());

        final var captor = ArgumentCaptor.forClass(UpdateAuthorRequest.class);
        verify(authorService).partialUpdate(eq(NUMBER), captor.capture(), eq(3L));
        assertEquals("stored.jpg", captor.getValue().getPhotoURI());
    }

    @Test
    void updateWithAStaleVersionReturns409() throws Exception {
        when(authorService.partialUpdate(eq(NUMBER), any(UpdateAuthorRequest.class), eq(1L)))
                .thenThrow(new org.hibernate.StaleObjectStateException("Author", NUMBER));

        mockMvc.perform(patch("/api/authors/" + NUMBER).header("If-Match", "\"1\"").param("name", "Maria José"))
                .andExpect(status().isConflict());
    }

    // find

    @Test
    void findByAuthorNumberReturns200WithEtag() throws Exception {
        existing(null);

        mockMvc.perform(get("/api/authors/" + NUMBER))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.name").value("João Alberto"))
                .andExpect(jsonPath("$.bio").value("Biografia de João Alberto."));
    }

    @Test
    void findByUnknownAuthorNumberReturns404() throws Exception {
        when(authorService.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByNameReturnsTheMappedAuthors() throws Exception {
        final var found = List.of(author(1L, "João Alberto", null), author(2L, "João Pedro", null));
        when(authorService.findByName("João")).thenReturn(found);
        when(authorViewMapper.toAuthorView(found)).thenReturn(found.stream().map(AuthorControllerTest::view).toList());

        mockMvc.perform(get("/api/authors").param("name", "João"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[1].name").value("João Pedro"));
    }

    // books

    @Test
    void booksOfAnUnknownAuthorReturn404() throws Exception {
        when(authorService.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/99/books"))
                .andExpect(status().isNotFound());

        verify(authorService, never()).findBooksByAuthorNumber(anyLong());
    }

    @Test
    void booksOfAnAuthorAreMappedToBookViews() throws Exception {
        existing(null);
        final List<Book> books = List.of(mock(Book.class));
        final var bookView = new BookView();
        bookView.setTitle("Encantos de contar");
        when(authorService.findBooksByAuthorNumber(NUMBER)).thenReturn(books);
        when(bookViewMapper.toBookView(books)).thenReturn(List.of(bookView));

        mockMvc.perform(get("/api/authors/" + NUMBER + "/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Encantos de contar"));
    }

    // top 5

    @Test
    void top5WithoutLendingsReturns404() throws Exception {
        when(authorService.findTopAuthorByLendings()).thenReturn(List.of());

        mockMvc.perform(get("/api/authors/top5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void top5ReturnsTheAuthorsAndTheirLendingCounts() throws Exception {
        when(authorService.findTopAuthorByLendings()).thenReturn(List.of(
                new AuthorLendingView("João Alberto", 7L), new AuthorLendingView("Maria José", 2L)));

        mockMvc.perform(get("/api/authors/top5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorName").value("João Alberto"))
                .andExpect(jsonPath("$.items[0].lendingCount").value(7))
                .andExpect(jsonPath("$.items[1].authorName").value("Maria José"));
    }

    // co-authors

    @Test
    void coAuthorsOfAnUnknownAuthorReturn404() throws Exception {
        when(authorService.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/99/coauthors"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eachCoAuthorIsMappedWithItsOwnBooks() throws Exception {
        final var author = existing(null);
        final var coAuthor = author(2L, "Maria José", null);
        final List<Book> coAuthorBooks = List.of(mock(Book.class));
        final var coAuthorView = new CoAuthorView("Maria José", Map.of(), List.of());
        when(authorService.findCoAuthorsByAuthorNumber(NUMBER)).thenReturn(List.of(coAuthor));
        when(authorService.findBooksByAuthorNumber(2L)).thenReturn(coAuthorBooks);
        when(authorViewMapper.toCoAuthorView(coAuthor, coAuthorBooks)).thenReturn(coAuthorView);
        when(authorViewMapper.toAuthorCoAuthorBooksView(eq(author), anyList()))
                .thenAnswer(i -> new AuthorCoAuthorBooksView(view(author), i.getArgument(1)));

        mockMvc.perform(get("/api/authors/" + NUMBER + "/coauthors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.name").value("João Alberto"))
                .andExpect(jsonPath("$.coauthors.length()").value(1))
                .andExpect(jsonPath("$.coauthors[0].name").value("Maria José"));

        verify(authorService).findBooksByAuthorNumber(2L);
        verify(authorService, never()).findBooksByAuthorNumber(NUMBER);
    }

    // photo

    @Test
    void photoOfAnUnknownAuthorReturns404() throws Exception {
        when(authorService.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/99/photo"))
                .andExpect(status().isNotFound());
    }

    @Test
    void photoOfAnAuthorWithoutPhotoReturns200WithoutBody() throws Exception {
        existing(null);

        mockMvc.perform(get("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));

        verify(fileStorageService, never()).getFile(anyString());
    }

    @Test
    void pngPhotoIsReturnedAsPng() throws Exception {
        final byte[] image = {1, 2, 3};
        existing("author.png");
        when(fileStorageService.getFile("author.png")).thenReturn(image);
        when(fileStorageService.getExtension("author.png")).thenReturn(Optional.of("png"));

        mockMvc.perform(get("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(image));
    }

    @Test
    void otherPhotosAreReturnedAsJpeg() throws Exception {
        final byte[] image = {4, 5};
        existing("author.jpg");
        when(fileStorageService.getFile("author.jpg")).thenReturn(image);
        when(fileStorageService.getExtension("author.jpg")).thenReturn(Optional.of("jpg"));

        mockMvc.perform(get("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(image));
    }

    @Test
    void photoWhoseFileIsMissingReturns200WithoutBody() throws Exception {
        existing("author.png");
        when(fileStorageService.getFile("author.png")).thenReturn(null);
        when(fileStorageService.getExtension("author.png")).thenReturn(Optional.of("png"));

        mockMvc.perform(get("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void photoWithoutExtensionReturns400() throws Exception {
        existing("author");
        when(fileStorageService.getFile("author")).thenReturn(new byte[]{1});
        when(fileStorageService.getExtension("author")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isBadRequest());
    }

    // delete photo

    @Test
    void deletePhotoOfAnUnknownAuthorReturns403() throws Exception {
        // As-is behaviour: the controller signals an unknown author with AccessDeniedException instead of a 404.
        when(authorService.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/authors/99/photo"))
                .andExpect(status().isForbidden());

        verify(authorService, never()).removeAuthorPhoto(anyLong(), anyLong());
    }

    @Test
    void deletePhotoOfAnAuthorWithoutPhotoReturns404() throws Exception {
        existing(null);

        mockMvc.perform(delete("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isNotFound());

        verify(fileStorageService, never()).deleteFile(anyString());
        verify(authorService, never()).removeAuthorPhoto(anyLong(), anyLong());
    }

    @Test
    void deletePhotoRemovesTheFileAndTheReference() throws Exception {
        existing("author.png");

        mockMvc.perform(delete("/api/authors/" + NUMBER + "/photo"))
                .andExpect(status().isOk());

        verify(fileStorageService).deleteFile("author.png");
        verify(authorService).removeAuthorPhoto(NUMBER, 3L);
    }
}
