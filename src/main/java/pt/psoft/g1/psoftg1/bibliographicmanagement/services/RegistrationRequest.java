package pt.psoft.g1.psoftg1.bibliographicmanagement.services;

import java.util.List;

/** Null means use retrieved data; explicit catalogue IDs are librarian choices. */
public record RegistrationRequest(
        String title, String description, String genre, List<Long> authorIds) {
    public static RegistrationRequest empty() {
        return new RegistrationRequest(null, null, null, null);
    }
}
