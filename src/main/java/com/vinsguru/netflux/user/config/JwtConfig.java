package com.vinsguru.netflux.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(RsaProperties properties) throws IOException, GeneralSecurityException {
        var privateKey = loadPrivateKey(properties);
        var publicKey = derivePublicKey(privateKey);
        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    }

    private RSAPrivateKey loadPrivateKey(RsaProperties properties) throws IOException {
        try (var inputStream = Files.newInputStream(properties.privateKey())) {
            return RsaKeyConverters.pkcs8().convert(inputStream);
        }
    }

    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws GeneralSecurityException {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalStateException("RSA private key must include the public exponent (CRT form)");
        }
        var spec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
