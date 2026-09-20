package com.example.secretencoder.steganography;

import com.example.secretencoder.encryption.AesGcmEncryptionService;
import com.example.secretencoder.exception.DecryptionException;
import com.example.secretencoder.exception.SteganographyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Text Steganography Service using invisible Unicode Zero-Width characters.
 * Encrypts the secret message with AES-256-GCM, converts the ciphertext into a bit stream,
 * and encodes it using non-printing Unicode characters (\u200B and \u200C), framed by markers (\u200D).
 * When embedded into normal carrier/cover text, the text remains visually unchanged.
 */
@Service
public class TextSteganographyService {

    // Zero-width Unicode characters
    private static final char ZW_ZERO = '\u200B';   // Zero-Width Space represents binary 0
    private static final char ZW_ONE = '\u200C';    // Zero-Width Non-Joiner represents binary 1
    private static final char ZW_START = '\u200D';  // Zero-Width Joiner marks payload start
    private static final char ZW_END = '\uFEFF';    // Zero-Width No-Break Space marks payload end

    private static final String DEFAULT_COVER_TEXT =
            "The quick brown fox jumps over the lazy dog. Today's security conference notes have been distributed.";

    private final AesGcmEncryptionService encryptionService;

    public TextSteganographyService(AesGcmEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public record TextEncodeResult(String encodedText, int coverLength, int hiddenPayloadBytes) {}

    /**
     * Encodes a secret message into cover text using AES-256-GCM and zero-width characters.
     */
    public TextEncodeResult encode(String coverText, String secretMessage, String password) {
        if (secretMessage == null || secretMessage.trim().isEmpty()) {
            throw new SteganographyException("Secret message cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new SteganographyException("Encryption password cannot be empty.");
        }

        String carrier = (coverText != null && !coverText.trim().isEmpty())
                ? coverText.trim()
                : DEFAULT_COVER_TEXT;

        // 1. Encrypt secret message with AES-256-GCM
        byte[] messageBytes = secretMessage.getBytes(StandardCharsets.UTF_8);
        AesGcmEncryptionService.EncryptedData encrypted = encryptionService.encrypt(messageBytes, password);
        byte[] packedBytes = encryptionService.pack(encrypted);

        // 2. Convert packed bytes into zero-width characters sequence
        StringBuilder zwSequence = new StringBuilder();
        zwSequence.append(ZW_START);

        for (byte b : packedBytes) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                zwSequence.append(bit == 1 ? ZW_ONE : ZW_ZERO);
            }
        }
        zwSequence.append(ZW_END);

        // 3. Embed invisibly into cover text (after the first space, or end of string)
        String finalCover;
        int firstSpace = carrier.indexOf(' ');
        if (firstSpace != -1) {
            finalCover = carrier.substring(0, firstSpace + 1) + zwSequence + carrier.substring(firstSpace + 1);
        } else {
            finalCover = carrier + zwSequence;
        }

        return new TextEncodeResult(finalCover, carrier.length(), packedBytes.length);
    }

    /**
     * Extracts and decrypts the hidden message from carrier text.
     */
    public String decode(String encodedText, String password) {
        if (encodedText == null || encodedText.isEmpty()) {
            throw new SteganographyException("Encoded text cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        // Locate ZW_START and ZW_END markers
        int startIndex = encodedText.indexOf(ZW_START);
        int endIndex = encodedText.indexOf(ZW_END, startIndex + 1);

        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex + 1) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        // Extract bits
        List<Byte> byteList = new ArrayList<>();
        int currentByte = 0;
        int bitCount = 0;

        for (int i = startIndex + 1; i < endIndex; i++) {
            char c = encodedText.charAt(i);
            if (c == ZW_ZERO) {
                currentByte = (currentByte << 1);
                bitCount++;
            } else if (c == ZW_ONE) {
                currentByte = (currentByte << 1) | 1;
                bitCount++;
            }

            if (bitCount == 8) {
                byteList.add((byte) currentByte);
                currentByte = 0;
                bitCount = 0;
            }
        }

        if (byteList.isEmpty()) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        byte[] packedBytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            packedBytes[i] = byteList.get(i);
        }

        // Unpack salt, IV, ciphertext
        AesGcmEncryptionService.EncryptedData unpacked = encryptionService.unpack(packedBytes);

        // Decrypt using password
        byte[] decrypted = encryptionService.decrypt(unpacked.salt(), unpacked.iv(), unpacked.ciphertext(), password);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
