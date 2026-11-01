package pt.psoft.g1.psoftg1.usermanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pt.psoft.g1.psoftg1.testutils.ApiIntegrationTestSupport;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Class SUT: UserAdminApi through UserService, the mappers and the user repository (custom search included).
 */
class UserAdminApiIntegrationTest extends ApiIntegrationTestSupport {
    private static final String USERS = "/api/admin/users";
    private Librarian admin;

    @BeforeEach
    void setUp() {
        admin = newLibrarian("admin.it@example.com");
    }

    @Test
    void adminCreatesReaderAndLibrarianAccounts() throws Exception {
        mockMvc.perform(post(USERS).with(asUser(admin, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("rita.it@example.com", "Rita Lopes", "READER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("rita.it@example.com"))
                .andExpect(jsonPath("$.fullName").value("Rita Lopes"));

        mockMvc.perform(post(USERS).with(asUser(admin, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("luis.it@example.com", "Luis Lopes", "LIBRARIAN")))
                .andExpect(status().isOk());
    }

    @Test
    void creatingAnExistingUsernameIsAConflict() throws Exception {
        mockMvc.perform(post(USERS).with(asUser(admin, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(admin.getUsername(), "Rita Lopes", "READER")))
                .andExpect(status().isConflict());
    }

    @Test
    void adminGetsUpdatesAndDisablesAUser() throws Exception {
        final User user = newReader("ana.it@example.com").getReader();

        mockMvc.perform(get(USERS + "/" + user.getId()).with(asUser(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("ana.it@example.com"));

        mockMvc.perform(put(USERS + "/" + user.getId()).with(asUser(admin, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Sofia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Sofia"));

        mockMvc.perform(delete(USERS + "/" + user.getId()).with(asUser(admin, "ADMIN")))
                .andExpect(status().isOk());
        assertFalse(userRepository.findById(user.getId()).orElseThrow().isEnabled());
    }

    @Test
    void searchByUsernameReturnsOnlyThatUser() throws Exception {
        newReader("bruno.it@example.com");
        newReader("carla.it@example.com");

        mockMvc.perform(post(USERS + "/search").with(asUser(admin, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"number\":1,\"limit\":10},\"query\":{\"username\":\"carla.it@example.com\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].username").value("carla.it@example.com"));
    }

    @Test
    void searchPagesTheResults() throws Exception {
        newReader("bruno.it@example.com");
        newReader("carla.it@example.com");

        mockMvc.perform(post(USERS + "/search").with(asUser(admin, "ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"number\":1,\"limit\":2},\"query\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void nonAdminCannotManageUsers() throws Exception {
        mockMvc.perform(get(USERS + "/" + admin.getId()).with(asUser(admin, "LIBRARIAN")))
                .andExpect(status().isForbidden());
    }

    private static String createBody(String username, String name, String role) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\",\"name\":\"" + name
                + "\",\"role\":\"" + role + "\"}";
    }
}
