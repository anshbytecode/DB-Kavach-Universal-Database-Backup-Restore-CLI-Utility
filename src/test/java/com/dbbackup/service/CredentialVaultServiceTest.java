package com.dbbackup.service;

import com.dbbackup.model.CredentialProfile;
import com.dbbackup.model.DbmsType;
import com.dbbackup.service.security.CredentialVaultService;
import com.dbbackup.service.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CredentialVaultServiceTest {

    private CredentialVaultService vaultService;
    private String masterPassword = "MasterVaultSecretPassword123!";

    @BeforeEach
    public void setUp(@TempDir Path tempDir) {
        EncryptionService encryptionService = new EncryptionService();
        vaultService = new CredentialVaultService(encryptionService);
        vaultService.setVaultFilePath(tempDir.resolve("test_vault.enc").toAbsolutePath().toString());
    }

    @Test
    public void testSaveAndGetProfile() throws Exception {
        CredentialProfile profile = new CredentialProfile();
        profile.setProfileName("prod-mysql");
        profile.setDbmsType(DbmsType.MYSQL);
        profile.setHost("prod-db.domain.com");
        profile.setPort(3306);
        profile.setDatabaseName("production");
        profile.setUsername("backup_user");
        profile.setPassword("SuperSecretPass123");

        vaultService.saveProfile(masterPassword, profile);

        CredentialProfile fetched = vaultService.getProfile(masterPassword, "prod-mysql");
        assertNotNull(fetched);
        assertEquals("prod-mysql", fetched.getProfileName());
        assertEquals(DbmsType.MYSQL, fetched.getDbmsType());
        assertEquals("prod-db.domain.com", fetched.getHost());
        assertEquals("SuperSecretPass123", fetched.getPassword());
    }

    @Test
    public void testListAndRemoveProfiles() throws Exception {
        CredentialProfile p1 = new CredentialProfile();
        p1.setProfileName("db1");
        p1.setDbmsType(DbmsType.SQLITE);
        p1.setDatabaseName("db1.sqlite");

        CredentialProfile p2 = new CredentialProfile();
        p2.setProfileName("db2");
        p2.setDbmsType(DbmsType.POSTGRESQL);
        p2.setDatabaseName("db2_pg");

        vaultService.saveProfile(masterPassword, p1);
        vaultService.saveProfile(masterPassword, p2);

        List<String> list = vaultService.listProfiles(masterPassword);
        assertEquals(2, list.size());
        assertTrue(list.contains("db1"));
        assertTrue(list.contains("db2"));

        boolean removed = vaultService.removeProfile(masterPassword, "db1");
        assertTrue(removed);

        List<String> updatedList = vaultService.listProfiles(masterPassword);
        assertEquals(1, updatedList.size());
        assertFalse(updatedList.contains("db1"));
    }

    @Test
    public void testVaultAccessFailsWithWrongMasterPassword() throws Exception {
        CredentialProfile profile = new CredentialProfile();
        profile.setProfileName("test-profile");
        profile.setDbmsType(DbmsType.MYSQL);
        profile.setDatabaseName("test_db");

        vaultService.saveProfile(masterPassword, profile);

        assertThrows(Exception.class, () -> {
            vaultService.getProfile("WrongMasterPassword", "test-profile");
        });
    }
}
