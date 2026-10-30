package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import java.net.URI;
import java.util.Arrays;
import java.util.ArrayList;
import pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure.BibliographicTransport;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import pt.psoft.g1.psoftg1.bibliographicmanagement.configuration.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

class BibliographicConfigurationTest {
    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(BibliographicConfiguration.class)
                    .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void sameConfigurationSupportsSingleAndMultipleSourcesWithoutNetwork() {
        // Arrange / Act / Assert: new startup binding for each setup; no lookup performed.
        for (String selection :
                new String[] {
                    "google-books", "open-library", "google-books,open-library", "open-library,google-books"
                })
            context.withPropertyValues("bibliographic.sources=" + selection)
                    .run(
                            c ->
                                    assertThat(c)
                                            .hasNotFailed()
                                            .hasSingleBean(BibliographicService.class));
    }

    @Test
    void startupSelectionControlsActualAdapterCallsAndInjectsConfiguredKey() throws Exception {
        for (String selection : new String[]{"google-books", "open-library",
                "google-books,open-library", "open-library,google-books"}) {
            // Arrange: real configuration/adapters/mappers; transport is controlled, no live calls.
            var calls = new ArrayList<String>();
            var transport = mock(BibliographicTransport.class);
            when(transport.execute(any())).thenAnswer(invocation -> {
                URI uri = invocation.getArgument(0);
                if (uri.getPath().equals("/google")) {
                    assertThat(uri.getRawQuery()).contains("key=fake-test-key");
                    calls.add("google-books");
                    return new BibliographicTransport.TransportResponse(200, "{\"totalItems\":0}");
                }
                assertThat(uri.getPath()).isEqualTo("/open");
                assertThat(uri.getRawQuery()).doesNotContain("key=");
                calls.add("open-library");
                return new BibliographicTransport.TransportResponse(200, "{}");
            });
            context.withBean("controlledTransport", BibliographicTransport.class,
                            () -> transport, definition -> definition.setPrimary(true))
                    .withPropertyValues("bibliographic.sources=" + selection,
                            "bibliographic.google-books.url=http://localhost/google",
                            "bibliographic.open-library.url=http://localhost/open",
                            "bibliographic.google-books.api-key=fake-test-key")
                    .run(c -> {
                        // Act
                        var result = c.getBean(BibliographicService.class).retrieve("9780132350884");
                        // Assert: only enabled sources, exactly once and in configured order.
                        assertThat(calls).containsExactlyElementsOf(Arrays.asList(selection.split(",")));
                        assertThat(result.status()).isEqualTo(LookupResult.Status.NOT_FOUND);
                        assertThat(result.sources()).extracting(SourceOutcome::source)
                                .containsExactlyElementsOf(calls);
                    });
        }
    }

    @Test
    void invalidSetupPreventsStartup() {
        // Arrange / Act / Assert
        for (String value : new String[] {"", "unknown", "google-books,google-books"})
            context.withPropertyValues("bibliographic.sources=" + value)
                    .run(c -> assertThat(c).hasFailed());
        context.run(c -> assertThat(c).hasFailed());
        context.withPropertyValues(
                        "bibliographic.sources=google-books", "bibliographic.timeout-ms=0")
                .run(c -> assertThat(c).hasFailed());
    }
}
