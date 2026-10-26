package pt.psoft.g1.psoftg1.authormanagement.model;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import static org.junit.jupiter.api.Assertions.*;

/** Transparent-box test of the {@link Author} aggregate: construction rules, versioned patch and photo removal. */
class AuthorTest {
    private static final String NAME = "João Alberto";
    private static final String BIO = "O João Alberto nasceu em Chaves e foi pedreiro a maior parte da sua vida.";

    private Author author(String photoURI) {
        return new Author(NAME, BIO, photoURI);
    }

    private static UpdateAuthorRequest patch(String name, String bio, String photoURI) {
        return new UpdateAuthorRequest(bio, name, null, photoURI);
    }

    // construction

    @Test
    void newAuthorKeepsItsNameBioAndPhoto() {
        final var subject = author("authorPhoto.jpg");

        assertEquals(NAME, subject.getName());
        assertEquals(BIO, subject.getBio());
        assertEquals("authorPhoto.jpg", subject.getPhoto().getPhotoFile());
        assertEquals(0L, subject.getVersion());
    }

    @Test
    void photoIsOptional() {
        assertNull(author(null).getPhoto());
    }

    @Test
    void aPhotoThatIsNotAValidPathIsDiscarded() {
        assertNull(author("invalid\u0000path.png").getPhoto());
    }

    @Test
    void idIsTheAuthorNumber() {
        final var subject = author(null);
        ReflectionTestUtils.setField(subject, "authorNumber", 7L);

        assertEquals(7L, subject.getAuthorNumber());
        assertEquals(7L, subject.getId());
    }

    @Test
    void nameIsMandatory() {
        assertThrows(IllegalArgumentException.class, () -> new Author(null, BIO, null));
        assertThrows(IllegalArgumentException.class, () -> new Author("   ", BIO, null));
    }

    @Test
    void nameOnlyAcceptsAlphanumericCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new Author("João <Alberto>", BIO, null));
    }

    @Test
    void bioIsMandatory() {
        assertThrows(IllegalArgumentException.class, () -> new Author(NAME, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Author(NAME, " ", null));
    }

    // applyPatch

    @Test
    void patchWithTheCurrentVersionChangesEveryFieldSent() {
        final var subject = author(null);

        subject.applyPatch(0, patch("Maria José", "Uma nova biografia.", "newPhoto.png"));

        assertEquals("Maria José", subject.getName());
        assertEquals("Uma nova biografia.", subject.getBio());
        assertEquals("newPhoto.png", subject.getPhoto().getPhotoFile());
    }

    @Test
    void patchOnlyChangesTheFieldsThatAreSent() {
        final var subject = author("authorPhoto.jpg");

        subject.applyPatch(0, patch(null, "Só muda a biografia.", null));

        assertEquals(NAME, subject.getName());
        assertEquals("Só muda a biografia.", subject.getBio());
        assertEquals("authorPhoto.jpg", subject.getPhoto().getPhotoFile());
    }

    @Test
    void patchOfTheNameAloneKeepsTheBio() {
        final var subject = author(null);

        subject.applyPatch(0, patch("Maria José", null, null));

        assertEquals("Maria José", subject.getName());
        assertEquals(BIO, subject.getBio());
        assertNull(subject.getPhoto());
    }

    @Test
    void patchWithAStaleVersionIsRejectedAndChangesNothing() {
        final var subject = author(null);

        assertThrows(StaleObjectStateException.class, () -> subject.applyPatch(1, patch("Maria José", "Outra.", "p.png")));

        assertEquals(NAME, subject.getName());
        assertEquals(BIO, subject.getBio());
        assertNull(subject.getPhoto());
    }

    @Test
    void patchWithAnInvalidNameIsRejected() {
        final var subject = author(null);

        assertThrows(IllegalArgumentException.class, () -> subject.applyPatch(0, patch("Nome$Inválido", null, null)));
    }

    // removePhoto

    @Test
    void removePhotoWithTheCurrentVersionClearsThePhoto() {
        final var subject = author("authorPhoto.jpg");

        subject.removePhoto(0);

        assertNull(subject.getPhoto());
    }

    @Test
    void removePhotoWithAStaleVersionIsRejectedAndKeepsThePhoto() {
        final var subject = author("authorPhoto.jpg");

        assertThrows(ConflictException.class, () -> subject.removePhoto(3));

        assertEquals("authorPhoto.jpg", subject.getPhoto().getPhotoFile());
    }
}
