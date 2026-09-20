package com.example.secretencoder.steganography;

import com.example.secretencoder.encryption.AesGcmEncryptionService;
import com.example.secretencoder.exception.CapacityExceededException;
import com.example.secretencoder.exception.DecryptionException;
import com.example.secretencoder.exception.InvalidFileException;
import com.example.secretencoder.exception.SteganographyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageSteganographyServiceTest {

    private ImageSteganographyService stegoService;

    @BeforeEach
    void setUp() {
        AesGcmEncryptionService encryptionService = new AesGcmEncryptionService();
        stegoService = new ImageSteganographyService(encryptionService);
    }

    private MockMultipartFile createPngMockFile(String name, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(45, 85, 125));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("StegoVault Test", 10, 20);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile(name, name + ".png", "image/png", baos.toByteArray());
    }

    @Test
    @DisplayName("Should successfully encode and decode message from PNG image")
    void testEncodeAndDecodeRoundTrip() throws Exception {
        MockMultipartFile originalPng = createPngMockFile("carrier", 120, 120);
        String secret = "Operation Blackbriar: Meet at rendezvous point Delta 0900 hrs.";
        String password = "TacticalPassword!2026";

        // 1. Encode
        byte[] encodedBytes = stegoService.encodeMessage(originalPng, secret, password);
        assertNotNull(encodedBytes);
        assertTrue(encodedBytes.length > 0);

        // 2. Decode with correct password
        MockMultipartFile encodedPng = new MockMultipartFile("encoded", "encoded.png", "image/png", encodedBytes);
        String decoded = stegoService.decodeMessage(encodedPng, password);

        assertEquals(secret, decoded);
    }

    @Test
    @DisplayName("Should fail to decode with incorrect password")
    void testDecodeWithIncorrectPassword() throws Exception {
        MockMultipartFile originalPng = createPngMockFile("carrier", 100, 100);
        String secret = "Classified secret.";
        String correctPassword = "CorrectKey123";
        String wrongPassword = "WrongKey999";

        byte[] encodedBytes = stegoService.encodeMessage(originalPng, secret, correctPassword);
        MockMultipartFile encodedPng = new MockMultipartFile("encoded", "encoded.png", "image/png", encodedBytes);

        DecryptionException exception = assertThrows(DecryptionException.class, () ->
                stegoService.decodeMessage(encodedPng, wrongPassword)
        );

        assertEquals("Unable to decrypt the hidden message.", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw CapacityExceededException when message exceeds image capacity")
    void testMessageTooLarge() throws Exception {
        // Very small image: 10 x 10 pixels = 100 pixels = 300 bits = 37 bytes total capacity
        // Our header alone requires 37 bytes + 16 bytes GCM tag = 53 bytes minimum
        MockMultipartFile tinyPng = createPngMockFile("tiny", 10, 10);
        String secret = "This message is definitely too large to fit inside this tiny 10x10 image!";

        assertThrows(CapacityExceededException.class, () ->
                stegoService.encodeMessage(tinyPng, secret, "password")
        );
    }

    @Test
    @DisplayName("Should reject invalid image or non-PNG format")
    void testInvalidImageFormat() {
        MockMultipartFile textFile = new MockMultipartFile(
                "image", "fake.png", "image/png", "This is plain text, not a PNG image!".getBytes()
        );

        assertThrows(InvalidFileException.class, () ->
                stegoService.encodeMessage(textFile, "secret", "password")
        );
    }

    @Test
    @DisplayName("Should reject empty secret message")
    void testEmptyMessage() throws Exception {
        MockMultipartFile originalPng = createPngMockFile("carrier", 100, 100);

        assertThrows(SteganographyException.class, () ->
                stegoService.encodeMessage(originalPng, "   ", "password")
        );
    }
}
