package com.gymflow.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.gymflow.api.error.BadRequestException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleTokenVerifierService {

    private final GoogleOAuthProperties properties;

    public GoogleIdentity verify(String idTokenValue) {
        List<String> audiences = properties.sanitizedAudiences();
        if (audiences.isEmpty()) {
            throw new BadRequestException("Google login is not configured");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
            ).setAudience(audiences).build();

            GoogleIdToken idToken = verifier.verify(idTokenValue);
            if (idToken == null) {
                throw new BadRequestException("Invalid Google token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BadRequestException("Google account email is not verified");
            }

            return new GoogleIdentity(
                payload.getSubject(),
                payload.getEmail(),
                (String) payload.get("name"),
                (String) payload.get("picture")
            );
        } catch (GeneralSecurityException | IOException ex) {
            throw new BadRequestException("Google token verification failed");
        }
    }

    public record GoogleIdentity(String googleSubject, String email, String displayName, String avatarUrl) {
    }
}
