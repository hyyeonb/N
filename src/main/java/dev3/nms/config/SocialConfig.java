package dev3.nms.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SocialConfig {

    // Kakao
    @Value("${social.kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${social.kakao.javascript-key}")
    private String kakaoJavascriptKey;

    @Value("${social.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    // Naver
    @Value("${social.naver.client-id}")
    private String naverClientId;

    @Value("${social.naver.client-secret}")
    private String naverClientSecret;

    @Value("${social.naver.redirect-uri}")
    private String naverRedirectUri;

    // Google
    @Value("${social.google.client-id}")
    private String googleClientId;

    @Value("${social.google.client-secret}")
    private String googleClientSecret;

    @Value("${social.google.redirect-uri}")
    private String googleRedirectUri;
}
