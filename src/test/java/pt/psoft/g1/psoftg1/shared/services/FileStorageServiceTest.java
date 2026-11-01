package pt.psoft.g1.psoftg1.shared.services;

import jakarta.validation.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.api.UploadFileResponse;
import pt.psoft.g1.psoftg1.shared.model.FileUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Class SUT: FileStorageService and FileUtils, on a temporary upload folder.
 */
class FileStorageServiceTest {
    private static final byte[] IMAGE = {1, 2, 3};

    @TempDir
    Path uploadDir;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        final FileStorageProperties properties = new FileStorageProperties();
        properties.setUploadDir(uploadDir.toString());
        properties.setPhotoMaxSize(100);
        service = new FileStorageService(properties);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest("POST", "/api/readers/photos")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static MockMultipartFile photo(String name, String contentType, byte[] content) {
        return new MockMultipartFile("photo", name, contentType, content);
    }

    @Test
    void storedFileCanBeReadLoadedAndDeleted() {
        final String fileName = service.storeFile("abc", photo("me.png", "image/png", IMAGE));

        assertThat(fileName).isEqualTo("abc.png");
        assertThat(service.getFile(fileName)).containsExactly(IMAGE);
        assertThat(service.loadFileAsResource(fileName).exists()).isTrue();

        service.deleteFile(fileName);
        assertThat(Files.exists(uploadDir.resolve(fileName))).isFalse();
        assertThat(service.getFile(fileName)).isNull();
    }

    @Test
    void missingFilesAreReportedAsNotFound() {
        assertThatThrownBy(() -> service.loadFileAsResource("nope.png")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deleteFile("nope.png")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.deleteFile(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extensionIsTakenAfterTheLastDot() {
        assertThat(service.getExtension("a.b.jpeg")).contains("jpeg");
        assertThat(service.getExtension("noextension")).isEmpty();
        assertThat(service.getExtension(null)).isEmpty();
    }

    @Test
    void validRequestPhotoIsStoredUnderAGeneratedName() {
        final String fileName = service.getRequestPhoto(photo("me.jpg", "image/jpeg", IMAGE));

        assertThat(fileName).endsWith(".jpg");
        assertThat(Files.exists(uploadDir.resolve(fileName))).isTrue();
    }

    @Test
    void noRequestPhotoMeansNoFile() {
        assertThat(service.getRequestPhoto(null)).isNull();
    }

    @Test
    void requestPhotoRejectsOversizedUnknownOrNonImageFiles() {
        assertThatThrownBy(() -> service.getRequestPhoto(photo("big.png", "image/png", new byte[101])))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.getRequestPhoto(photo("me.png", null, IMAGE)))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.getRequestPhoto(photo("me.gif", "image/gif", IMAGE)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void uploadResponsePointsToTheSingleFileUrl() throws Exception {
        final UploadFileResponse response = FileUtils.doUploadFile(service, "id1", photo("me.png", "image/png", IMAGE));

        assertThat(response.getFileName()).isEqualTo("id1.png");
        assertThat(response.getFileDownloadUri()).endsWith("/api/readers/photo/id1.png");
        assertThat(response.getSize()).isEqualTo(IMAGE.length);
    }

    @Test
    void uploadWithoutAFileFails() {
        assertThatThrownBy(() -> FileUtils.doUploadFile(service, "id1", null)).isInstanceOf(Exception.class);
    }
}
