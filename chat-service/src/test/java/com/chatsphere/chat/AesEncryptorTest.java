package com.chatsphere.chat;

import com.chatsphere.common.crypto.AesEncryptor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AesEncryptorTest {

    @Test
    public void testMessageEncryptionDecryption() {
        AesEncryptor encryptor = new AesEncryptor();
        String plainText = "ChatSphere X - Enterprise Distributed System Message!";

        // Encrypt
        String encryptedText = encryptor.convertToDatabaseColumn(plainText);
        assertNotNull(encryptedText);
        assertNotEquals(plainText, encryptedText);
        
        // Decrypt
        String decryptedText = encryptor.convertToEntityAttribute(encryptedText);
        assertEquals(plainText, decryptedText);
    }

    @Test
    public void testNullHandling() {
        AesEncryptor encryptor = new AesEncryptor();
        assertNull(encryptor.convertToDatabaseColumn(null));
        assertNull(encryptor.convertToEntityAttribute(null));
    }
}
