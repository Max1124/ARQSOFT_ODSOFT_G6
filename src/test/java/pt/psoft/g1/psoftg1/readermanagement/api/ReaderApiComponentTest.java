package pt.psoft.g1.psoftg1.readermanagement.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opaque-box collaboration SUT: real controller, services, mappers, domain and H2 repositories.
 * Only the remote quote gateway is replaced. Fixtures roll back after each test. */
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reader-component-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.profiles.active=test",
        "file.upload-dir=target/reader-component-uploads"})
class ReaderApiComponentTest extends ApiIntegrationTestSupport {
    @MockBean ApiNinjasService quotes;

    @Test void registrationPersistsReaderAndExistingGenreAssociation() throws Exception {
        // Arrange
        newGenre("ReaderInterest");
        // Act
        var result = mockMvc.perform(post("/api/readers")
                .param("username", "reader.component@example.com").param("password", PASSWORD)
                .param("fullName", "Maria Silva").param("birthDate", "2000-01-01")
                .param("phoneNumber", "912345678").param("gdpr", "true")
                .param("interestList", "ReaderInterest"));
        // Assert
        result.andExpect(status().isCreated()).andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.email").value("reader.component@example.com"))
                .andExpect(jsonPath("$.interestList[0]").value("ReaderInterest"));
        var saved = readerRepository.findByUsername("reader.component@example.com").orElseThrow();
        assertThat(saved.getPhoneNumber()).isEqualTo("912345678");
        assertThat(saved.getInterestList()).extracting(g -> g.getGenre()).containsExactly("ReaderInterest");
        verifyNoInteractions(quotes);
    }

    @Test void unknownInterestDoesNotCreateAnAccount() throws Exception {
        // Arrange
        String email = "unknown.interest@example.com";
        // Act
        var result = mockMvc.perform(post("/api/readers")
                .param("username", email).param("password", PASSWORD).param("fullName", "Maria Silva")
                .param("birthDate", "2000-01-01").param("phoneNumber", "912345678")
                .param("gdpr", "true").param("interestList", "NonexistentGenre"));
        // Assert
        result.andExpect(status().isNotFound());
        assertThat(userRepository.findByUsername(email)).isEmpty();
        assertThat(readerRepository.findByUsername(email)).isEmpty();
        verifyNoInteractions(quotes);
    }

    @Test void readerLookupUsesControlledQuoteAndRealPersistedData() throws Exception {
        // Arrange
        var librarian = newLibrarian("reader.lookup.lib@example.com");
        var reader = newReader("reader.lookup@example.com");
        when(quotes.getRandomEventFromYearMonth(2000, 1)).thenReturn("Controlled event");
        // Act
        var result = mockMvc.perform(get("/api/readers/" + reader.getReaderNumber())
                .with(asUser(librarian, "LIBRARIAN")));
        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.quote").value("Controlled event"))
                .andExpect(jsonPath("$.email").value("reader.lookup@example.com"));
        verify(quotes).getRandomEventFromYearMonth(2000, 1);
    }

    @Test void readerCannotAccessAnotherReadersPhoto() throws Exception {
        // Arrange
        var owner = newReader("photo.owner@example.com");
        var other = newReader("photo.other@example.com");
        // Act
        var result = mockMvc.perform(get("/api/readers/" + other.getReaderNumber() + "/photo")
                .with(asUser(owner.getReader(), "READER")));
        // Assert
        result.andExpect(status().isForbidden());
        verifyNoInteractions(quotes);
    }

    @Test void patchRequiresConcurrencyHeaderAndLeavesReaderUnchanged() throws Exception {
        // Arrange
        var reader = newReader("patch.reader@example.com");
        // Act
        var result = mockMvc.perform(patch("/api/readers").param("phoneNumber", "923456789")
                .with(asUser(reader.getReader(), "READER")));
        // Assert
        result.andExpect(status().isBadRequest());
        assertThat(readerRepository.findByUsername("patch.reader@example.com").orElseThrow()
                .getPhoneNumber()).isEqualTo("919191919");
    }
}
