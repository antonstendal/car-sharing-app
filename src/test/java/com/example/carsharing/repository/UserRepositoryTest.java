package com.example.carsharing.repository;

import com.example.carsharing.model.Role;
import com.example.carsharing.model.User;
import com.example.carsharing.repository.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@ImportAutoConfiguration(classes = LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {"classpath:database/user/clean/remove-all.sql",
        "classpath:database/user/add-users-and-roles.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return user with roles when email matches")
    void findByEmailWithRoles_userExists_returnsUserWithRoles() {
        Optional<User> actual = userRepository.findByEmailWithRoles("manager@gmail.com");

        assertTrue(actual.isPresent());
        assertAll(
                () -> assertEquals(1, actual.get().getId()),
                () -> assertEquals("Bob", actual.get().getFirstName()),
                () -> assertEquals("Thornton", actual.get().getLastName()),
                () -> assertEquals(
                        "$2a$10$eomvkdaTLl.ZyuZgR.nq5eKqUAmahk9q82L/fYEKSE6J6FD.ldvIe",
                        actual.get().getPassword()),
                () -> assertEquals(1, actual.get().getRoles().size()),
                () -> assertTrue(actual.get().getRoles().stream()
                        .anyMatch(r -> r.getRole() == Role.RoleName.MANAGER)));
    }

    @Test
    @DisplayName("findByEmail: should return empty Optional when no user matches email")
    void findByEmailWithRoles_userDoesNotExist_returnsEmpty() {
        Optional<User> actual = userRepository.findByEmailWithRoles("testdata@gmail.com");
        assertTrue(actual.isEmpty());
    }

    @Test
    @DisplayName("Should return user when email matches")
    void findByEmail_userExists_returnsUser() {
        Optional<User> actual = userRepository.findByEmail("customer@gmail.com");

        assertTrue(actual.isPresent());
        assertAll(
                () -> assertEquals(2, actual.get().getId()),
                () -> assertEquals("Alice", actual.get().getFirstName()),
                () -> assertEquals("Harrison", actual.get().getLastName()),
                () -> assertEquals(
                        "$2a$10$eomvkdaTLl.ZyuZgR.nq5eKqUAmahk9q82L/fYEKSE6J6FD.ldvIe",
                        actual.get().getPassword()));
    }

    @Test
    @DisplayName("Should return empty Optional when no user matches email")
    void findByEmail_userDoesNotExist_returnsEmpty() {
        Optional<User> actual = userRepository.findByEmail("notexistedemail@gmail.com");
        assertTrue(actual.isEmpty());
    }
}
