package pt.psoft.g1.psoftg1.readermanagement.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import java.util.List;

/** Collaboration: generated mapper, its base helpers and real domain fixture, with a local web context. */
class ReaderQuoteMappingTest {
    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes(); }
    @Test void quoteViewPreservesIdentityConsentsInterestsAndPhotoLink() {
        // Arrange
        var request = new MockHttpServletRequest();
        request.setScheme("http"); request.setServerName("localhost"); request.setServerPort(8080);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        var reader=Reader.newReader("mapping@example.org","Passw0rd!123","Ana Silva");
        var details=new ReaderDetails(7,reader,"2000-03-04","919191919",true,true,false,null,List.of(new Genre("History")));
        var mapper=new ReaderViewMapperImpl();
        // Act
        var view=mapper.toReaderQuoteView(details);
        // Assert
        assertEquals("Ana Silva",view.getFullName()); assertEquals("mapping@example.org",view.getEmail());
        assertEquals("2000-03-04",view.getBirthDate()); assertEquals("919191919",view.getPhoneNumber());
        assertEquals(details.getReaderNumber(),view.getReaderNumber());
        assertEquals(List.of("History"),view.getInterestList());
        assertEquals("http://localhost:8080/api/readers/"+details.getReaderNumber()+"/photo",view.getPhoto());
        assertTrue(view.isGdprConsent()); assertTrue(view.isMarketingConsent()); assertFalse(view.isThirdPartySharingConsent());
        assertNull(view.getQuote()); // The external quote is added by the controller, not invented by mapping.
        assertNull(mapper.toReaderQuoteView(null));
    }
}
