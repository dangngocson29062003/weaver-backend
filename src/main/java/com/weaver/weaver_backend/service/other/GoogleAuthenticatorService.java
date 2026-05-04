package com.weaver.weaver_backend.service.other;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthenticatorService {
    private final GoogleAuthenticator ggAuth = new GoogleAuthenticator();
    public GoogleAuthenticatorKey createCredentials() {
        return ggAuth.createCredentials();
    }
    public String getKey(GoogleAuthenticatorKey googleAuthenticatorKey){
        return googleAuthenticatorKey.getKey();
    }
    public String getQRBarUrl(String email, GoogleAuthenticatorKey key) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL("Weaver Application", email, key);
    }
    public boolean verifyCode(String secret, int otp) {
        return ggAuth.authorize(secret, otp);
    }
}
