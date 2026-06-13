package utils;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Cipher;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@Tag("unit")
class AesUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"hello", "", "A longer string with spaces and symbols! @#$%^&*()"})
    void shouldDecryptToOriginalInput(String input) throws Exception {
        assumeTrue(isCipherAvailable());

        byte[] key = "1234567890abcdef".getBytes();
        byte[] encrypted = AesUtil.encrypt(input, key);
        String decrypted = AesUtil.decrypt(encrypted, key);
        assertEquals(input, decrypted);
    }

    private boolean isCipherAvailable() {
        try {
            Cipher.getInstance("AES");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}