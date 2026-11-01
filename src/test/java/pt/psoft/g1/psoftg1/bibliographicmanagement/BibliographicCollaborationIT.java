package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.util.*;

/** Real source selection, coordinator and merge collaboration. */
class BibliographicCollaborationIT {
    static final String ISBN = "9780132350884";
    BibliographicData data(String title, String desc, List<String> authors, List<String> genres) {
        return new BibliographicData(ISBN, title, desc, authors, genres, Map.of());
    }

    @Test
    void thirdSourceWorksThroughRealSelectionCoordinatorAndMerge() {
        // Arrange: third implementation has no changes to the existing coordinator or merge.
        var existing = mock(BibliographicSource.class);
        var third = mock(BibliographicSource.class);
        when(third.lookupByIsbn(ISBN))
                .thenReturn(
                        SourceOutcome.found(
                                "third", data("Third title", null, List.of(), List.of())));
        var installed =
                List.of(
                        new SourceRegistration("existing", existing),
                        new SourceRegistration("third", third));
        var selected = new SourceSelection().select("third", installed);
        // Act: real collaboration, not a single-class isolation test.
        var result = new BibliographicService(selected, new BibliographicMerge()).retrieve(ISBN);
        // Assert
        assertEquals("Third title", result.data().title());
        verify(third).lookupByIsbn(ISBN);
        verifyNoInteractions(existing);
    }
}
