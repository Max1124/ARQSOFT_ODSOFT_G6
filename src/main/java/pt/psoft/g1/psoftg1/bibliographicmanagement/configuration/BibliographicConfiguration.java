package pt.psoft.g1.psoftg1.bibliographicmanagement.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import pt.psoft.g1.psoftg1.bibliographicmanagement.infrastructure.*;
import pt.psoft.g1.psoftg1.bibliographicmanagement.services.*;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Configuration
public class        BibliographicConfiguration {
    @Bean
    BibliographicTransport bibliographicTransport(
            @Value("${bibliographic.timeout-ms:5000}") long timeout) {
        if (timeout < 1 || timeout > 60000)
            throw new IllegalArgumentException("bibliographic.timeout-ms must be 1..60000");
        var duration = Duration.ofMillis(timeout);
        return new HttpBibliographicTransport(
                HttpClient.newBuilder().connectTimeout(duration).build(), duration);
    }

    @Bean
    SourceRegistration googleBooks(
            BibliographicTransport transport,
            ObjectMapper json,
            @Value("${bibliographic.google-books.url:https://www.googleapis.com/books/v1/volumes}")
                    String endpoint) {
        return new SourceRegistration(
                "google-books",
                new GoogleBooksAdapter(transport, new GoogleBooksMapper(json), endpoint));
    }

    @Bean
    SourceRegistration openLibrary(
            BibliographicTransport transport,
            ObjectMapper json,
            @Value("${bibliographic.open-library.url:https://openlibrary.org/api/books}")
                    String endpoint) {
        return new SourceRegistration(
                "open-library",
                new OpenLibraryAdapter(transport, new OpenLibraryMapper(json), endpoint));
    }

    @Bean
    BibliographicMerge bibliographicMerge() {
        return new BibliographicMerge();
    }

    @Bean
    BibliographicService bibliographicService(
            @Value("${bibliographic.sources}") String sources,
            List<SourceRegistration> installed,
            BibliographicMerge merge) {
        return new BibliographicService(new SourceSelection().select(sources, installed), merge);
    }
}
