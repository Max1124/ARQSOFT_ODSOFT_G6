package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.util.*;

/** Opaque class contracts; data records are fixtures, behavioural dependencies are doubles. */
class BibliographicCoreTest {
    static final String ISBN = "9780132350884";

    BibliographicData data(String title, String desc, List<String> authors, List<String> genres) {
        return new BibliographicData(ISBN, title, desc, authors, genres, Map.of());
    }

    @Test
    void priorityFallbackProvenanceAndAuthorConflict() {
        // Arrange
        var first =
                SourceOutcome.found(
                        "first", data("First", null, List.of("Alice"), List.of(" Fiction ")));
        var second =
                SourceOutcome.found(
                        "second",
                        data(
                                "Other",
                                "Description",
                                List.of("Bob"),
                                List.of("fiction", "History")));
        // Act
        var result = new BibliographicMerge().combine(ISBN, List.of(first, second));
        // Assert
        assertEquals("First", result.data().title());
        assertEquals("Description", result.data().description());
        assertEquals(List.of("Alice"), result.data().authors());
        assertEquals(List.of("Fiction", "History"), result.data().genres());
        assertEquals("second", result.data().provenance().get("description"));
        assertEquals(
                List.of("Author list differs in second; first available list retained"),
                result.warnings());
    }

    @Test
    void failureDoesNotDiscardSuccessfulPartialData() {
        // Arrange
        var found = SourceOutcome.found("first", data(null, null, List.of(), List.of()));
        // Act
        var result =
                new BibliographicMerge()
                        .combine(ISBN, List.of(found, SourceOutcome.failure("second", "timeout")));
        // Assert
        assertEquals(LookupResult.Status.FOUND, result.status());
        assertNotNull(result.data());
        assertEquals(List.of("Source second failed; lookup incomplete"), result.warnings());
    }

    @Test
    void noMatchDiffersFromFailedLookup() {
        // Arrange
        var merge = new BibliographicMerge();
        var absent = SourceOutcome.notFound("first");
        // Act / Assert
        assertEquals(LookupResult.Status.NOT_FOUND, merge.combine(ISBN, List.of(absent)).status());
        assertEquals(
                LookupResult.Status.FAILURE,
                merge.combine(ISBN, List.of(absent, SourceOutcome.failure("second", "error")))
                        .status());
    }

    @Test
    void coordinatorCallsOnlyInjectedSourcesAndDelegatesMerge() {
        // Arrange
        var source = mock(BibliographicSource.class);
        var disabled = mock(BibliographicSource.class);
        var merge = mock(BibliographicMerge.class);
        var outcome = SourceOutcome.notFound("one");
        when(source.lookupByIsbn(ISBN)).thenReturn(outcome);
        var expected =
                new LookupResult(LookupResult.Status.NOT_FOUND, null, List.of(outcome), List.of());
        when(merge.combine(ISBN, List.of(outcome))).thenReturn(expected);
        // Act
        var result = new BibliographicService(List.of(source), merge).retrieve(ISBN);
        // Assert
        assertSame(expected, result);
        verify(source).lookupByIsbn(ISBN);
        verifyNoInteractions(disabled);
    }

    @Test
    void invalidIsbnNeverCallsProviders() {
        // Arrange
        var source = mock(BibliographicSource.class);
        var merge = mock(BibliographicMerge.class);
        var service = new BibliographicService(List.of(source), merge);
        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.retrieve("bad"));
        verifyNoInteractions(source, merge);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "unknown", "a,a", "a,", "A"})
    void invalidSelectionRejected(String value) {
        // Arrange
        var selection = new SourceSelection();
        var source = mock(BibliographicSource.class);
        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> selection.select(value, List.of(new SourceRegistration("a", source))));
        verifyNoInteractions(source);
    }

    @Test
    void extensionAndOrderDoNotRequireCoordinatorChanges() {
        // Arrange: a third fake provider is registered like any installed provider.
        var a = mock(BibliographicSource.class);
        var b = mock(BibliographicSource.class);
        var third = mock(BibliographicSource.class);
        // Act
        var selected =
                new SourceSelection()
                        .select(
                                " third, a ",
                                List.of(
                                        new SourceRegistration("a", a),
                                        new SourceRegistration("b", b),
                                        new SourceRegistration("third", third)));
        // Assert
        assertEquals(List.of(third, a), selected);
        assertThrows(UnsupportedOperationException.class, () -> selected.add(b));
        verifyNoInteractions(a, b, third);
    }

    @Test
    void duplicateRegistrationRejected() {
        // Arrange
        var source = mock(BibliographicSource.class);
        // Act / Assert
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SourceSelection()
                                .select(
                                        "a",
                                        List.of(
                                                new SourceRegistration("a", source),
                                                new SourceRegistration("a", source))));
    }

    @Test
    void blankFieldsDoNotBlockLaterSourceAndDoNotClaimProvenance() {
        // Arrange
        var first = SourceOutcome.found("first", data(" ", "", List.of(), List.of()));
        var second =
                SourceOutcome.found(
                        "second", data("Title", "Description", List.of("Alice"), List.of()));
        // Act
        var result = new BibliographicMerge().combine(ISBN, List.of(first, second));
        // Assert
        assertEquals("Title", result.data().title());
        assertEquals("Description", result.data().description());
        assertEquals("second", result.data().provenance().get("title"));
        assertEquals("second", result.data().provenance().get("description"));
        assertEquals(List.of("Alice"), result.data().authors());
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
