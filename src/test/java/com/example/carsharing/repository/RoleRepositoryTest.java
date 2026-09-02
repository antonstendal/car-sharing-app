package com.example.carsharing.repository;

import com.example.carsharing.model.Role;
import com.example.carsharing.repository.user.RoleRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DataJpaTest
@ImportAutoConfiguration(classes = LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = {"classpath:database/role/clean/remove-all.sql",
        "classpath:database/role/add-roles.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class RoleRepositoryTest {
    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Should return role when role name matches")
    void findByRole_roleExists_returnsRole() {
        Optional<Role> actual = roleRepository.findByRole(Role.RoleName.CUSTOMER);

        assertTrue(actual.isPresent());
        assertEquals(Role.RoleName.CUSTOMER, actual.get().getRole());
    }

    @Test
    @DisplayName("Should return empty Optional when role name does not exist")
    void findByRole_roleDoesNotExist_returnsEmpty() {
        Optional<Role> actual = roleRepository.findByRole(Role.RoleName.MANAGER);
        assertTrue(actual.isEmpty());
    }
}
