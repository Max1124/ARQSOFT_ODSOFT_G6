package pt.psoft.g1.psoftg1.usermanagement.services;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Class SUT: UserService. Repositories and the password encoder are doubles, so only the service rules are under test.
 */
class UserServiceTest {
    private static final String USERNAME = "ana@example.com";
    private final UserRepository users = mock(UserRepository.class);
    private final ForbiddenNameRepository forbiddenNames = mock(ForbiddenNameRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(users, mock(EditUserMapper.class), forbiddenNames, encoder);

    private static CreateUserRequest request(String name, String role) {
        final CreateUserRequest request = new CreateUserRequest(USERNAME, name, "Passw0rd!123");
        request.setRole(role);
        return request;
    }

    private static Jwt jwt(String subject) {
        return Jwt.withTokenValue("t").header("alg", "none").subject(subject).build();
    }

    @Test
    void createEncodesThePasswordAndSaves() {
        when(users.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(encoder.encode(anyString())).thenReturn("$2a$10$EncodedHashValue");
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));

        final User user = service.create(request("Ana Maria", "READER"));

        assertThat(user).isInstanceOf(Reader.class);
        verify(encoder).encode("Passw0rd!123");
        assertThat(new BCryptPasswordEncoder().matches("$2a$10$EncodedHashValue", user.getPassword())).isTrue();
    }

    @Test
    void createRejectsNamesWithAForbiddenWord() {
        when(users.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(forbiddenNames.findByForbiddenNameIsContained("Bad")).thenReturn(List.of(new ForbiddenName("Bad")));

        assertThatThrownBy(() -> service.create(request("Ana Bad", "READER")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(users, never()).save(any());
    }

    @Test
    void createWithAnUnknownRoleSavesNothing() {
        when(users.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThat(service.create(request("Ana Maria", "ADMIN"))).isNull();
        verify(users, never()).save(any());
    }

    @Test
    void loadUserByUsernameFailsForUnknownUsers() {
        when(users.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername(USERNAME)).isInstanceOf(UsernameNotFoundException.class);
        assertThat(service.usernameExists(USERNAME)).isFalse();
    }

    @Test
    void searchUsesDefaultPageAndQueryWhenMissing() {
        service.searchUsers(null, null);

        verify(users).searchUsers(eq(new Page(1, 10)), any(SearchUsersQuery.class));
    }

    @Test
    void namesAreSearchedExactlyOrByFragment() {
        service.findByName("Ana");
        service.findByNameLike("An");

        verify(users).findByNameName("Ana");
        verify(users).findByNameNameContains("An");
    }

    @Test
    void authenticatedUserComesFromTheJwtSubject() {
        final User ana = Reader.newReader(USERNAME, "Passw0rd!123", "Ana Maria");
        when(users.findByUsername(USERNAME)).thenReturn(Optional.of(ana));

        assertThat(service.getAuthenticatedUser(new JwtAuthenticationToken(jwt("1," + USERNAME)))).isSameAs(ana);
    }

    @Test
    void anonymousOrUnknownCallersAreDenied() {
        when(users.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAuthenticatedUser(null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAuthenticatedUser(new UsernamePasswordAuthenticationToken("x", "y")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.getAuthenticatedUser(new JwtAuthenticationToken(jwt("9,ghost@example.com"))))
                .isInstanceOf(AccessDeniedException.class);
    }
}
