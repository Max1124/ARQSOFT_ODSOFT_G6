package pt.psoft.g1.psoftg1.authormanagement.services;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class-level opaque-box test of {@link AuthorServiceImpl}: repositories are doubles and the (pure) MapStruct mapper
 * is the real one, so no Spring context or database is needed (CON-TST-10).
 */
class AuthorServiceImplTest {
    private static final long AUTHOR_NUMBER = 1L;

    private AuthorRepository authorRepository;
    private BookRepository bookRepository;
    private PhotoRepository photoRepository;
    private AuthorServiceImpl service;

    @BeforeEach
    void setUp() {
        authorRepository = mock(AuthorRepository.class);
        bookRepository = mock(BookRepository.class);
        photoRepository = mock(PhotoRepository.class);
        service = new AuthorServiceImpl(authorRepository, bookRepository, new AuthorMapperImpl(), photoRepository);

        when(authorRepository.save(any(Author.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Author existingAuthor(String photoURI) {
        final var author = new Author("João Alberto", "O João Alberto nasceu em Chaves e foi pedreiro.", photoURI);
        ReflectionTestUtils.setField(author, "authorNumber", AUTHOR_NUMBER);
        when(authorRepository.findByAuthorNumber(AUTHOR_NUMBER)).thenReturn(Optional.of(author));
        return author;
    }

    private static UpdateAuthorRequest update(String name, String bio, MultipartFile photo, String photoURI) {
        return new UpdateAuthorRequest(bio, name, photo, photoURI);
    }

    // create

    @Test
    void createPersistsTheAuthorWithoutPhoto() {
        final var created = service.create(new CreateAuthorRequest("Maria José", "A Maria José nasceu em Viseu.", null, null));

        assertEquals("Maria José", created.getName());
        assertEquals("A Maria José nasceu em Viseu.", created.getBio());
        assertNull(created.getPhoto());
        verify(authorRepository).save(created);
    }

    @Test
    void createKeepsThePhotoWhenAFileAndItsStoredNameArePresent() {
        final var created = service.create(new CreateAuthorRequest("Maria José", "Bio.", mock(MultipartFile.class), "stored.png"));

        assertEquals("stored.png", created.getPhoto().getPhotoFile());
    }

    @Test
    void createIgnoresAPhotoUriWithoutAnUploadedFile() {
        final var request = new CreateAuthorRequest("Maria José", "Bio.", null, "../../etc/passwd");

        final var created = service.create(request);

        assertNull(created.getPhoto());
        assertNull(request.getPhotoURI());
    }

    @Test
    void createIgnoresAnUploadedFileThatWasNotStored() {
        final var request = new CreateAuthorRequest("Maria José", "Bio.", mock(MultipartFile.class), null);

        final var created = service.create(request);

        assertNull(created.getPhoto());
        assertNull(request.getPhoto());
    }

    @Test
    void createWithAnInvalidNameIsRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateAuthorRequest("Maria@José", "Bio.", null, null)));

        verify(authorRepository, never()).save(any());
    }

    // partialUpdate

    @Test
    void updateOfAnUnknownAuthorIsNotFoundAndNothingIsSaved() {
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.partialUpdate(99L, update("Novo", null, null, null), 0));

        verify(authorRepository, never()).save(any());
    }

    @Test
    void updateAppliesTheSentFieldsAndSaves() {
        final var author = existingAuthor(null);

        final var updated = service.partialUpdate(AUTHOR_NUMBER, update("Maria José", null, null, null), 0);

        assertSame(author, updated);
        assertEquals("Maria José", updated.getName());
        assertEquals("O João Alberto nasceu em Chaves e foi pedreiro.", updated.getBio());
        verify(authorRepository).save(author);
    }

    @Test
    void updateWithAStaleVersionIsRejectedAndNothingIsSaved() {
        existingAuthor(null);

        assertThrows(StaleObjectStateException.class,
                () -> service.partialUpdate(AUTHOR_NUMBER, update("Maria José", null, null, null), 5));

        verify(authorRepository, never()).save(any());
    }

    @Test
    void updateIgnoresAPhotoUriWithoutAnUploadedFile() {
        existingAuthor("old.png");
        final var request = update(null, null, null, "../../etc/passwd");

        final var updated = service.partialUpdate(AUTHOR_NUMBER, request, 0);

        assertEquals("old.png", updated.getPhoto().getPhotoFile());
        assertNull(request.getPhotoURI());
    }

    @Test
    void updateIgnoresAnUploadedFileThatWasNotStored() {
        existingAuthor("old.png");
        final var request = update(null, null, mock(MultipartFile.class), null);

        final var updated = service.partialUpdate(AUTHOR_NUMBER, request, 0);

        assertEquals("old.png", updated.getPhoto().getPhotoFile());
        assertNull(request.getPhoto());
    }

    @Test
    void updateReplacesThePhotoWhenAFileAndItsStoredNameArePresent() {
        existingAuthor("old.png");

        final var updated = service.partialUpdate(AUTHOR_NUMBER, update(null, null, mock(MultipartFile.class), "new.png"), 0);

        assertEquals("new.png", updated.getPhoto().getPhotoFile());
    }

    // queries

    @Test
    void findAllReturnsEveryAuthorOfTheRepository() {
        final var authors = List.of(existingAuthor(null));
        when(authorRepository.findAll()).thenReturn(authors);

        assertEquals(authors, service.findAll());
    }

    @Test
    void findByAuthorNumberReturnsTheAuthorOrEmpty() {
        final var author = existingAuthor(null);
        when(authorRepository.findByAuthorNumber(2L)).thenReturn(Optional.empty());

        assertEquals(Optional.of(author), service.findByAuthorNumber(AUTHOR_NUMBER));
        assertTrue(service.findByAuthorNumber(2L).isEmpty());
    }

    @Test
    void findByNameSearchesByTheBeginningOfTheName() {
        final var authors = List.of(existingAuthor(null));
        when(authorRepository.searchByNameNameStartsWith("Jo")).thenReturn(authors);

        assertEquals(authors, service.findByName("Jo"));
        verify(authorRepository, never()).searchByNameName(anyString());
    }

    @Test
    void topAuthorsAreTheFirstPageOfFive() {
        final var views = List.of(new AuthorLendingView("João Alberto", 4L));
        when(authorRepository.findTopAuthorByLendings(PageRequest.of(0, 5))).thenReturn(new PageImpl<>(views));

        assertEquals(views, service.findTopAuthorByLendings());

        final var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(authorRepository).findTopAuthorByLendings(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void booksAndCoAuthorsComeFromTheirRepositories() {
        final var book = mock(Book.class);
        final var coAuthor = existingAuthor(null);
        when(bookRepository.findBooksByAuthorNumber(AUTHOR_NUMBER)).thenReturn(List.of(book));
        when(authorRepository.findCoAuthorsByAuthorNumber(AUTHOR_NUMBER)).thenReturn(List.of(coAuthor));

        assertEquals(List.of(book), service.findBooksByAuthorNumber(AUTHOR_NUMBER));
        assertEquals(List.of(coAuthor), service.findCoAuthorsByAuthorNumber(AUTHOR_NUMBER));
    }

    // removeAuthorPhoto

    @Test
    void removePhotoOfAnUnknownAuthorIsNotFound() {
        when(authorRepository.findByAuthorNumber(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.removeAuthorPhoto(99L, 0));

        verify(photoRepository, never()).deleteByPhotoFile(anyString());
    }

    @Test
    void removePhotoClearsTheReferenceSavesAndDeletesThePhotoRecord() {
        final var author = existingAuthor("old.png");

        final var result = service.removeAuthorPhoto(AUTHOR_NUMBER, 0);

        assertEquals(Optional.of(author), result);
        assertNull(author.getPhoto());
        verify(authorRepository).save(author);
        verify(photoRepository).deleteByPhotoFile("old.png");
    }

    @Test
    void removePhotoWithAStaleVersionKeepsEverything() {
        final var author = existingAuthor("old.png");

        assertThrows(ConflictException.class, () -> service.removeAuthorPhoto(AUTHOR_NUMBER, 2));

        assertEquals("old.png", author.getPhoto().getPhotoFile());
        verify(authorRepository, never()).save(any());
        verify(photoRepository, never()).deleteByPhotoFile(anyString());
    }
}
