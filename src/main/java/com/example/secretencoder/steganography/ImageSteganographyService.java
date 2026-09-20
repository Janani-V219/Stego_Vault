package com.example.secretencoder.steganography;

import com.example.secretencoder.encryption.AesGcmEncryptionService;
import com.example.secretencoder.exception.CapacityExceededException;
import com.example.secretencoder.exception.DecryptionException;
import com.example.secretencoder.exception.InvalidFileException;
import com.example.secretencoder.exception.SteganographyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * High-performance, secure LSB (Least Significant Bit) Image Steganography Service.
 * Embeds AES-256-GCM encrypted secret payloads into the least significant bits of
 * Red, Green, and Blue channels of PNG images (3 bits per pixel), preserving the Alpha channel.
 */
@Service
public class ImageSteganographyService {

    // Magic bytes: "STEG"
    private static final byte[] MAGIC_HEADER = new byte[] { 'S', 'T', 'E', 'G' };
    private static final byte PROTOCOL_VERSION = 0x01;

    // Header structure:
    // [MAGIC: 4B] + [VERSION: 1B] + [SALT: 16B] + [IV: 12B] + [CIPHERTEXT_LENGTH: 4B]
    public static final int HEADER_OVERHEAD_BYTES = 4 + 1 + AesGcmEncryptionService.SALT_LENGTH_BYTES
            + AesGcmEncryptionService.IV_LENGTH_BYTES + 4; // 37 bytes
    public static final int GCM_TAG_BYTES = AesGcmEncryptionService.TAG_LENGTH_BITS / 8; // 16 bytes

    // PNG Magic signature bytes: 89 50 4E 47 0D 0A 1A 0A
    private static final byte[] PNG_SIGNATURE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private final AesGcmEncryptionService encryptionService;

    public ImageSteganographyService(AesGcmEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /**
     * Calculates the maximum safe data storage capacity of an image in bytes.
     * Each pixel provides 3 bits (1 bit in R, 1 bit in G, 1 bit in B).
     */
    public long calculateMaxCapacityBytes(int width, int height) {
        return ((long) width * height * 3) / 8;
    }

    /**
     * Estimates required payload size in bytes for a given secret message length.
     */
    public long calculateRequiredBytes(int messageUtf8ByteLength) {
        return HEADER_OVERHEAD_BYTES + messageUtf8ByteLength + GCM_TAG_BYTES;
    }

    /**
     * Validates if an uploaded file is a valid PNG image.
     */
    public void validatePngImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select an image file to upload.");
        }

        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".png")) {
            throw new InvalidFileException("Unsupported file type. Only PNG images are supported for lossless steganography.");
        }

        try (InputStream is = file.getInputStream()) {
            byte[] signature = new byte[8];
            int read = is.read(signature);
            if (read != 8 || !Arrays.equals(signature, PNG_SIGNATURE)) {
                throw new InvalidFileException("Invalid PNG file format. The file signature does not match a valid PNG image.");
            }
        } catch (IOException e) {
            throw new InvalidFileException("Failed to read the uploaded image.");
        }
    }

    /**
     * Reads and converts an uploaded image into an ARGB BufferedImage suitable for pixel manipulation.
     */
    public BufferedImage readAndNormalizeImage(MultipartFile file) {
        validatePngImage(file);
        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                throw new InvalidFileException("The uploaded file could not be decoded as a valid image.");
            }

            // Normalize to TYPE_INT_ARGB to guarantee consistent 32-bit pixel data across formats
            BufferedImage normalized = new BufferedImage(
                    original.getWidth(),
                    original.getHeight(),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g2d = normalized.createGraphics();
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();
            return normalized;
        } catch (IOException e) {
            throw new InvalidFileException("Error reading image file: " + e.getMessage());
        }
    }

    /**
     * Encodes a secret message into an image using AES-256-GCM and LSB steganography.
     * Returns the encoded image as PNG byte array.
     */
    public byte[] encodeMessage(MultipartFile imageFile, String secretMessage, String password) {
        if (secretMessage == null || secretMessage.trim().isEmpty()) {
            throw new SteganographyException("Secret message cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new SteganographyException("Encryption password cannot be empty.");
        }

        BufferedImage image = readAndNormalizeImage(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        long maxCapacity = calculateMaxCapacityBytes(width, height);
        byte[] messageBytes = secretMessage.getBytes(StandardCharsets.UTF_8);
        long requiredCapacity = calculateRequiredBytes(messageBytes.length);

        if (requiredCapacity > maxCapacity) {
            throw new CapacityExceededException(
                    "Message is too large for this image. Maximum capacity is " +
                    (maxCapacity / 1024) + " KB, but required capacity is " +
                    (requiredCapacity / 1024) + " KB. Please choose a larger image.",
                    maxCapacity,
                    requiredCapacity
            );
        }

        // 1. Encrypt message with password-derived AES-256-GCM
        AesGcmEncryptionService.EncryptedData encrypted = encryptionService.encrypt(messageBytes, password);

        // 2. Assemble complete binary payload:
        // [MAGIC 4B] + [VERSION 1B] + [SALT 16B] + [IV 12B] + [CIPHERTEXT_LENGTH 4B] + [CIPHERTEXT]
        ByteBuffer payloadBuffer = ByteBuffer.allocate(HEADER_OVERHEAD_BYTES + encrypted.ciphertext().length);
        payloadBuffer.put(MAGIC_HEADER);
        payloadBuffer.put(PROTOCOL_VERSION);
        payloadBuffer.put(encrypted.salt());
        payloadBuffer.put(encrypted.iv());
        payloadBuffer.putInt(encrypted.ciphertext().length);
        payloadBuffer.put(encrypted.ciphertext());

        byte[] fullPayload = payloadBuffer.array();

        // 3. Embed bits into RGB channels using LSB
        embedPayloadIntoImage(image, fullPayload);

        // 4. Export as lossless PNG
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new SteganographyException("Failed to generate encoded PNG image: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts and decrypts a hidden secret message from an encoded image.
     */
    public String decodeMessage(MultipartFile imageFile, String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        BufferedImage image = readAndNormalizeImage(imageFile);
        int width = image.getWidth();
        int height = image.getHeight();

        long maxCapacity = calculateMaxCapacityBytes(width, height);
        if (maxCapacity < HEADER_OVERHEAD_BYTES + GCM_TAG_BYTES) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        // Extract header
        PixelBitReader bitReader = new PixelBitReader(image);
        byte[] headerBytes = bitReader.readBytes(HEADER_OVERHEAD_BYTES);

        // Verify Magic header
        for (int i = 0; i < MAGIC_HEADER.length; i++) {
            if (headerBytes[i] != MAGIC_HEADER[i]) {
                throw new DecryptionException("Unable to decrypt the hidden message.");
            }
        }

        // Verify Version
        byte version = headerBytes[4];
        if (version != PROTOCOL_VERSION) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
        headerBuffer.position(5); // Skip magic and version

        byte[] salt = new byte[AesGcmEncryptionService.SALT_LENGTH_BYTES];
        headerBuffer.get(salt);

        byte[] iv = new byte[AesGcmEncryptionService.IV_LENGTH_BYTES];
        headerBuffer.get(iv);

        int ciphertextLength = headerBuffer.getInt();

        // Validate ciphertext length against maximum possible image payload
        if (ciphertextLength <= 0 || (HEADER_OVERHEAD_BYTES + ciphertextLength) > maxCapacity) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        // Extract ciphertext bytes
        byte[] ciphertext = bitReader.readBytes(ciphertextLength);

        // Decrypt using AES-256-GCM
        byte[] decryptedBytes = encryptionService.decrypt(salt, iv, ciphertext, password);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Embeds payload bytes into image pixels (R, G, B LSBs).
     */
    private void embedPayloadIntoImage(BufferedImage image, byte[] payload) {
        int width = image.getWidth();
        int height = image.getHeight();

        int payloadByteIndex = 0;
        int payloadBitIndex = 0; // 0 to 7 (from MSB to LSB)
        int totalBytes = payload.length;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (payloadByteIndex >= totalBytes) {
                    break outer;
                }

                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = argb & 0xFF;

                // Embed into Red LSB
                if (payloadByteIndex < totalBytes) {
                    int bit = (payload[payloadByteIndex] >> (7 - payloadBitIndex)) & 1;
                    red = (red & 0xFE) | bit;
                    payloadBitIndex++;
                    if (payloadBitIndex == 8) {
                        payloadBitIndex = 0;
                        payloadByteIndex++;
                    }
                }

                // Embed into Green LSB
                if (payloadByteIndex < totalBytes) {
                    int bit = (payload[payloadByteIndex] >> (7 - payloadBitIndex)) & 1;
                    green = (green & 0xFE) | bit;
                    payloadBitIndex++;
                    if (payloadBitIndex == 8) {
                        payloadBitIndex = 0;
                        payloadByteIndex++;
                    }
                }

                // Embed into Blue LSB
                if (payloadByteIndex < totalBytes) {
                    int bit = (payload[payloadByteIndex] >> (7 - payloadBitIndex)) & 1;
                    blue = (blue & 0xFE) | bit;
                    payloadBitIndex++;
                    if (payloadBitIndex == 8) {
                        payloadBitIndex = 0;
                        payloadByteIndex++;
                    }
                }

                int newArgb = (alpha << 24) | (red << 16) | (green << 8) | blue;
                image.setRGB(x, y, newArgb);
            }
        }
    }

    /**
     * Helper bit reader that traverses image pixels and extracts LSBs from R, G, B channels.
     */
    private static class PixelBitReader {
        private final BufferedImage image;
        private final int width;
        private final int height;
        private int x = 0;
        private int y = 0;
        private int channel = 0; // 0: Red, 1: Green, 2: Blue
        private int currentArgb;

        public PixelBitReader(BufferedImage image) {
            this.image = image;
            this.width = image.getWidth();
            this.height = image.getHeight();
            if (width > 0 && height > 0) {
                this.currentArgb = image.getRGB(0, 0);
            }
        }

        public byte[] readBytes(int count) {
            byte[] bytes = new byte[count];
            for (int b = 0; b < count; b++) {
                int currentByte = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int bitValue = nextBit();
                    currentByte = (currentByte << 1) | bitValue;
                }
                bytes[b] = (byte) currentByte;
            }
            return bytes;
        }

        private int nextBit() {
            if (y >= height) {
                return 0; // Padding if out of bounds
            }

            int bit = 0;
            if (channel == 0) {
                int red = (currentArgb >> 16) & 0xFF;
                bit = red & 1;
                channel = 1;
            } else if (channel == 1) {
                int green = (currentArgb >> 8) & 0xFF;
                bit = green & 1;
                channel = 2;
            } else {
                int blue = currentArgb & 0xFF;
                bit = blue & 1;
                channel = 0;
                // Move to next pixel
                x++;
                if (x >= width) {
                    x = 0;
                    y++;
                }
                if (y < height) {
                    currentArgb = image.getRGB(x, y);
                }
            }
            return bit;
        }
    }
}
