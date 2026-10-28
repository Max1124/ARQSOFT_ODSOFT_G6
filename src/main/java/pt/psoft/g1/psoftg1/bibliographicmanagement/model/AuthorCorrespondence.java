package pt.psoft.g1.psoftg1.bibliographicmanagement.model;

import jakarta.persistence.*;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

@Entity
public class    AuthorCorrespondence {
    @Id private String id;

    @Column(nullable = false)
    private Long authorId;

    protected AuthorCorrespondence() {}

    public AuthorCorrespondence(String source, String isbn, String externalName, Long authorId) {
        if (authorId == null || authorId < 1)
            throw new IllegalArgumentException("A catalogue author is required");
        this.id = key(source, isbn, externalName);
        this.authorId = authorId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public static String key(String source, String isbn, String name) {
        if (source == null
                || source.isBlank()
                || isbn == null
                || isbn.isBlank()
                || name == null
                || name.isBlank())
            throw new IllegalArgumentException(
                    "Source, ISBN and external author name are required");
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            (source + "\n" + isbn + "\n" + name.strip())
                                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
