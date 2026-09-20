package com.example.secretencoder.steganography;

import com.example.secretencoder.encryption.AesGcmEncryptionService;
import com.example.secretencoder.exception.DecryptionException;
import com.example.secretencoder.exception.SteganographyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextSteganographyServiceTest {

    private TextSteganographyService textService;

    @BeforeEach
    void setUp() {
        AesGcmEncryptionService encryptionService = new AesGcmEncryptionService();
        textService = new TextSteganographyService(encryptionService);
    }

    @Test
    @DisplayName("Should encode secret message into cover text and decode successfully")
    void testEncodeAndDecodeText() {
        String cover = "The meeting will happen tomorrow in conference room B.";
        String secret = "Bring the documents.";
        String password = "MeetingPassword123";

        TextSteganographyService.TextEncodeResult result = textService.encode(cover, secret, password);
        assertNotNull(result.encodedText());
        assertFalse(result.encodedText().isEmpty());

        // Decode with correct password
        String decoded = textService.decode(result.encodedText(), password);
        assertEquals(secret, decoded);
    }

    @Test
    @DisplayName("Should fail when decoding with incorrect password")
    void testDecodeWithWrongPassword() {
        String cover = "Routine announcement regarding the quarterly review.";
        String secret = "Confidential financial figures.";
        String password = "SecretPassKey1";
        String wrongPassword = "WrongPassKey2";

        TextSteganographyService.TextEncodeResult result = textService.encode(cover, secret, password);

        assertThrows(DecryptionException.class, () ->
                textService.decode(result.encodedText(), wrongPassword)
        );
    }

    @Test
    @DisplayName("Should throw DecryptionException when input text has no hidden stego payload")
    void testDecodeNormalTextWithoutPayload() {
        String plainText = "This is ordinary text with zero hidden payload.";
        String password = "SomePassword";

        assertThrows(DecryptionException.class, () ->
                textService.decode(plainText, password)
        );
    }

    @Test
    @DisplayName("Should throw SteganographyException when secret message is empty")
    void testEncodeEmptySecret() {
        String cover = "Some cover text.";
        assertThrows(SteganographyException.class, () ->
                textService.encode(cover, "  ", "password")
        );
    }
}
