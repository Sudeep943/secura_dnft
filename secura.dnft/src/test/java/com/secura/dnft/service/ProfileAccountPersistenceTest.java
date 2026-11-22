package com.secura.dnft.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.secura.dnft.dao.ProfileAccountRepository;
import com.secura.dnft.entity.ProfileAccount;
import com.secura.dnft.entity.ProfileAccountId;

import jakarta.persistence.EntityManager;

@DataJpaTest
@ContextConfiguration(classes = ProfileAccountPersistenceTest.Config.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ProfileAccountPersistenceTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = ProfileAccount.class)
    @EnableJpaRepositories(basePackageClasses = ProfileAccountRepository.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
                    pattern = "com\\.secura\\.dnft\\.dao\\.(?!ProfileAccountRepository$).*"))
    static class Config {
    }

    @Autowired
    private ProfileAccountRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsCompositeKeyAndAuditTimestamps() {
        ProfileAccount account = new ProfileAccount();
        account.setProfileId("P1");
        account.setApartmentId("A1");
        account.setCreateBy("creator");
        account.setLastUpdtBy("creator");
        account.setJobeposition("manager");
        repository.saveAndFlush(account);
        entityManager.clear();

        ProfileAccount saved = repository.findById(new ProfileAccountId("P1", "A1")).orElseThrow();
        assertEquals("manager", saved.getJobeposition());
        assertNotNull(saved.getCreatedOn());
        assertNotNull(saved.getLstUpdtOn());

        saved.setLastUpdtBy("editor");
        saved.setFlatId("F1");
        repository.saveAndFlush(saved);
        entityManager.clear();

        ProfileAccount updated = repository.findById(new ProfileAccountId("P1", "A1")).orElseThrow();
        assertEquals("creator", updated.getCreateBy());
        assertEquals("editor", updated.getLastUpdtBy());
        assertEquals("F1", updated.getFlatId());
        assertEquals(saved.getCreatedOn(), updated.getCreatedOn());
    }
}
