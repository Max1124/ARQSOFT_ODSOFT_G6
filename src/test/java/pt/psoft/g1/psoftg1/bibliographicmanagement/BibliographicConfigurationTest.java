package pt.psoft.g1.psoftg1.bibliographicmanagement;

import static org.assertj.core.api.Assertions.assertThat;

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
                    "open-library", "google-books,open-library", "open-library,google-books"
                })
            context.withPropertyValues("bibliographic.sources=" + selection)
                    .run(
                            c ->
                                    assertThat(c)
                                            .hasNotFailed()
                                            .hasSingleBean(BibliographicService.class));
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
