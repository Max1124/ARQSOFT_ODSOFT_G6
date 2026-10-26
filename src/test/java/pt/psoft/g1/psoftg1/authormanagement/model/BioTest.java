package pt.psoft.g1.psoftg1.authormanagement.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Transparent-box test of the {@link Bio} value object: mandatory, bounded length and sanitised HTML. */
class BioTest {
    private static final int MAX_LENGTH = 4096;

    @Test
    void ensureBioMustNotBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new Bio(null));
    }

    @Test
    void ensureBioMustNotBeBlank() {
        assertThrows(IllegalArgumentException.class, () -> new Bio(""));
        assertThrows(IllegalArgumentException.class, () -> new Bio("   \n\t"));
    }

    @Test
    void bioWithTheMaximumLengthIsAccepted() {
        final String text = "a".repeat(MAX_LENGTH);

        assertEquals(text, new Bio(text).toString());
    }

    @Test
    void bioOneCharacterOverTheMaximumIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Bio("a".repeat(MAX_LENGTH + 1)));
    }

    @Test
    void ensureBioIsSet() {
        final var bio = new Bio("Some bio");
        assertEquals("Some bio", bio.toString());
    }

    @Test
    void ensureBioIsChanged() {
        final var bio = new Bio("Some bio");
        bio.setBio("Some other bio");
        assertEquals("Some other bio", bio.toString());
    }

    @Test
    void anInvalidChangeKeepsThePreviousBio() {
        final var bio = new Bio("Some bio");

        assertThrows(IllegalArgumentException.class, () -> bio.setBio(" "));

        assertEquals("Some bio", bio.toString());
    }

    @Test
    void scriptsAreRemovedButFormattingIsKept() {
        final var bio = new Bio("<b>Escritor</b> do Porto<script>alert('x')</script>");

        assertTrue(bio.toString().contains("<b>Escritor</b>"));
        assertFalse(bio.toString().contains("<script>"));
        assertFalse(bio.toString().contains("alert"));
    }
}
