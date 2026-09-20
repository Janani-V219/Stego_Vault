package com.example.secretencoder.encryption;

import com.example.secretencoder.exception.DecryptionException;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * Service providing AES-256-GCM authenticated encryption and decryption.
 * Uses PBKDF2WithHmacSHA256 for key derivation from user-supplied passwords,
 * with cryptographically secure random salts (16 bytes) and nonces/IVs (12 bytes).
 */
@Service
public class AesGcmEncryptionService {

    public static final int SALT_LENGTH_BYTES = 16;
    public static final int IV_LENGTH_BYTES = 12; // Recommended standard for GCM
    public static final int TAG_LENGTH_BITS = 128; // 16 bytes authentication tag
    public static final int KEY_LENGTH_BITS = 256;
    public static final int PBKDF2_ITERATIONS = 65536;

    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();

    public record EncryptedData(byte[] salt, byte[] iv, byte[] ciphertext) {}

    /**
     * Encrypts plaintext bytes using a password-derived AES-256-GCM key.
     * Generates a fresh random salt and IV for every call.
     */
    public EncryptedData encrypt(byte[] plaintext, String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Encryption password cannot be empty");
        }
        if (plaintext == null) {
            throw new IllegalArgumentException("Plaintext cannot be null");
        }

        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            secureRandom.nextBytes(salt);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            SecretKey secretKey = deriveKey(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext);
            return new EncryptedData(salt, iv, ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error occurred", e);
        }
    }

    /**
     * Decrypts ciphertext using the password and the provided salt and IV.
     * If the password is wrong or ciphertext is altered, throws DecryptionException
     * with a generic error message to prevent padding/oracle attacks.
     */
    public byte[] decrypt(byte[] salt, byte[] iv, byte[] ciphertext, String password) {
        if (password == null || password.isEmpty()) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }
        if (salt == null || salt.length != SALT_LENGTH_BYTES ||
            iv == null || iv.length != IV_LENGTH_BYTES ||
            ciphertext == null || ciphertext.length < (TAG_LENGTH_BITS / 8)) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        try {
            SecretKey secretKey = deriveKey(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            // Decryption failure (AEADBadTagException, BadPaddingException, etc.)
            // Never leak whether the password was close or what stage failed.
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }
    }

    /**
     * Packs [Salt (16B)] + [IV (12B)] + [Ciphertext + Tag] into a contiguous byte array.
     */
    public byte[] pack(EncryptedData data) {
        ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH_BYTES + IV_LENGTH_BYTES + data.ciphertext().length);
        buffer.put(data.salt());
        buffer.put(data.iv());
        buffer.put(data.ciphertext());
        return buffer.array();
    }

    /**
     * Unpacks [Salt (16B)] + [IV (12B)] + [Ciphertext + Tag] from a contiguous byte array.
     */
    public EncryptedData unpack(byte[] packedData) {
        int minLength = SALT_LENGTH_BYTES + IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8);
        if (packedData == null || packedData.length < minLength) {
            throw new DecryptionException("Unable to decrypt the hidden message.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(packedData);
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        buffer.get(salt);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] ciphertext = new byte[packedData.length - SALT_LENGTH_BYTES - IV_LENGTH_BYTES];
        buffer.get(ciphertext);

        return new EncryptedData(salt, iv, ciphertext);
    }

    private SecretKey deriveKey(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
