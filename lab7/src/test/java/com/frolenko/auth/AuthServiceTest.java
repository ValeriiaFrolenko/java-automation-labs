package com.frolenko.auth;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository repository;

    @InjectMocks
    AuthService authService;

    @Test
    void shouldReturnUserWhenRegisterSuccess() {
        String username = "test";
        String email = "test@test";
        String password = "test";

        when(repository.existsByUsername(username)).thenReturn(false);
        when(repository.existsByEmail(email)).thenReturn(false);
        when(repository.save(any())).thenReturn(new User(1L, username, email, "hash", "salt", User.Role.USER));

        User user = authService.register(username, email, password);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(user).isNotNull();
            softly.assertThat(user.username()).isEqualTo(username);
            softly.assertThat(user.email()).isEqualTo(email);
            softly.assertThat(user.role()).isEqualTo(User.Role.USER);
            softly.assertThat(user.passwordHash()).isNotEqualTo(password);
        });

        verify(repository, times(1)).save(any());
    }

    @Test
    void shouldReturnExceptionWhenUserExists() {
        when(repository.existsByUsername("test")).thenReturn(true);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> authService.register("test", "test@test", "test"));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnExceptionWhenEmailExists() {
        when(repository.existsByUsername("test")).thenReturn(false);
        when(repository.existsByEmail("test@test")).thenReturn(true);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> authService.register("test", "test@test", "test"));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnRegisteredUsernames(){
        when(repository.findAll()).thenReturn(List.of(
                new User(1L, "user1", "user1@test", "hash1", "salt1", User.Role.USER),
                new User(2L, "user2", "user2@test", "hash2", "salt2", User.Role.USER),
                new User(3L, "user3", "user3@test", "hash3", "salt3", User.Role.USER)
        ));
        List<String> result = authService.getRegisteredUsernames();
        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("user1", "user2", "user3")
                .doesNotContain("user4");
    }

}