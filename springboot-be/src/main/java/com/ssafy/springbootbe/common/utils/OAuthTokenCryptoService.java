package com.ssafy.springbootbe.common.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class OAuthTokenCryptoService {

    private final TextEncryptor textEncryptor;

    public OAuthTokenCryptoService(
            @Value("${app.crypto.password}") String password,
            @Value("${app.crypto.salt}") String salt
    ) {
        // AES-GCM 기반
        this.textEncryptor = Encryptors.delux(password, salt);
        // 또는 BytesEncryptor stronger(...)를 직접 써도 됩니다.
    }

    public String encrypt(String plainText) {
        return textEncryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        return textEncryptor.decrypt(encryptedText);
    }
}
