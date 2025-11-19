package com.teamloci.loci.config.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class AppleTokenVerifier {

    private final String appleIss;
    private final String appleAudience;
    private final String appleKeysUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AppleTokenVerifier(
            @Value("${apple.iss}") String appleIss,
            @Value("${apple.audience}") String appleAudience,
            @Value("${apple.keys-url}") String appleKeysUrl
    ) {
        this.appleIss = appleIss;
        this.appleAudience = appleAudience;
        this.appleKeysUrl = appleKeysUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Claims verify(String identityToken) {
        try {
            String headerJson = new String(Base64.getUrlDecoder().decode(identityToken.substring(0, identityToken.indexOf('.'))));
            Map<String, String> header = objectMapper.readValue(headerJson, Map.class);
            String kid = header.get("kid");
            String alg = header.get("alg");

            ApplePublicKeys applePublicKeys = restTemplate.getForObject(appleKeysUrl, ApplePublicKeys.class);
            ApplePublicKeys.ApplePublicKey matchingKey = applePublicKeys.getMatchingKey(kid, alg);

            PublicKey publicKey = createPublicKey(matchingKey);

            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer(appleIss)
                    .requireAudience(appleAudience)
                    .build()
                    .parseClaimsJws(identityToken)
                    .getBody();

        } catch (Exception e) {
        throw new CustomException(ErrorCode.APPLE_TOKEN_VERIFY_FAILED, e);
    }
    }

    private PublicKey createPublicKey(ApplePublicKeys.ApplePublicKey key) throws Exception {
        byte[] nBytes = Base64.getUrlDecoder().decode(key.getN());
        byte[] eBytes = Base64.getUrlDecoder().decode(key.getE());

        BigInteger n = new BigInteger(1, nBytes);
        BigInteger e = new BigInteger(1, eBytes);

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
        KeyFactory keyFactory = KeyFactory.getInstance(key.getKty());
        return keyFactory.generatePublic(publicKeySpec);
    }

    private static class ApplePublicKeys {
        private List<ApplePublicKey> keys;

        public List<ApplePublicKey> getKeys() { return keys; }
        public void setKeys(List<ApplePublicKey> keys) { this.keys = keys; }

        public ApplePublicKey getMatchingKey(String kid, String alg) {
            return keys.stream()
                    .filter(key -> key.getKid().equals(kid) && key.getAlg().equals(alg))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("일치하는 Apple 공개 키를 찾을 수 없습니다."));
        }

        private static class ApplePublicKey {
            private String kty;
            private String kid;
            private String use;
            private String alg;
            private String n;
            private String e;

            public String getKty() { return kty; }
            public void setKty(String kty) { this.kty = kty; }
            public String getKid() { return kid; }
            public void setKid(String kid) { this.kid = kid; }
            public String getUse() { return use; }
            public void setUse(String use) { this.use = use; }
            public String getAlg() { return alg; }
            public void setAlg(String alg) { this.alg = alg; }
            public String getN() { return n; }
            public void setN(String n) { this.n = n; }
            public String getE() { return e; }
            public void setE(String e) { this.e = e; }
        }
    }
}