package com.example.secretencoder.encryption;

import com.example.secretencoder.exception.DecryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmEncryptionServiceTest {

    private AesGcmEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new AesGcmEncryptionService();
    }

    @Test
    @DisplayName("Should successfully encrypt and decrypt payload with correct password")
    void testEncryptDecryptRoundTrip() {
        String secret = "TopSecretCybersecurityMessage123!#$";
        String password = "StrongMasterPassword@2026";

        AesGcmEncryptionService.EncryptedData encrypted =
                encryptionService.encrypt(secret.getBytes(StandardCharsets.UTF_8), password);

        assertNotNull(encrypted.salt());
        assertEquals(AesGcmEncryptionService.SALT_LENGTH_BYTES, encrypted.salt().length);
        assertNotNull(encrypted.iv());
        assertEquals(AesGcmEncryptionService.IV_LENGTH_BYTES, encrypted.iv().length);
        assertNotNull(encrypted.ciphertext());
        assertTrue(encrypted.ciphertext().length > secret.length());

        byte[] decrypted = encryptionService.decrypt(
                encrypted.salt(),
                encrypted.iv(),
                encrypted.ciphertext(),
                password
        );

        assertEquals(secret, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should throw DecryptionException when password is incorrect")
    void testDecryptWithWrongPassword() {
        String secret = "Confidential Information";
        String correctPassword = "CorrectPassword123";
        String wrongPassword = "WrongPassword999";

        AesGcmEncryptionService.EncryptedData encrypted =
                encryptionService.encrypt(secret.getBytes(StandardCharsets.UTF_8), correctPassword);

        DecryptionException exception = assertThrows(DecryptionException.class, () ->
                encryptionService.decrypt(
                        encrypted.salt(),
                        encrypted.iv(),
                        encrypted.ciphertext(),
                        wrongPassword
                )
        );

        assertEquals("Unable to decrypt the hidden message.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw DecryptionException when ciphertext is tampered with")
    void testDecryptWithTamperedCiphertext() {
        String secret = "Integrity check test";
        String password = "SafePassword";

        AesGcmEncryptionService.EncryptedData encrypted =
                encryptionService.encrypt(secret.getBytes(StandardCharsets.UTF_8), password);

        // Tamper with one byte in ciphertext
        byte[] tamperedCiphertext = encrypted.ciphertext().clone();
        tamperedCiphertext[0] ^= 0xFF;

        assertThrows(DecryptionException.class, () ->
                encryptionService.decrypt(
                        encrypted.salt(),
                        encrypted.iv(),
                        tamperedCiphertext,
                        password
                )
        );
    }

    @Test
    @DisplayName("Should pack and unpack encrypted payload properly")
    void testPackAndUnpack() {
        String secret = "Packing Payload Test";
        String password = "PackPassword123";

        AesGcmEncryptionService.EncryptedData encrypted =
                encryptionService.encrypt(secret.getBytes(StandardCharsets.UTF_8), password);

        byte[] packed = encryptionService.pack(encrypted);
        assertNotNull(packed);

        AesGcmEncryptionService.EncryptedData unpacked = encryptionService.unpack(packed);
        assertArrayEquals(encrypted.salt(), unpacked.salt());
        assertArrayEquals(encrypted.iv(), unpacked.iv());
        assertArrayEquals(encrypted.ciphertext(), unpacked.ciphertext());

        byte[] decrypted = encryptionService.decrypt(
                unpacked.salt(),
                unpacked.iv(),
                unpacked.ciphertext(),
                password
        );
        assertEquals(secret, new String(decrypted, StandardCharsets.UTF_8));
    }
}
