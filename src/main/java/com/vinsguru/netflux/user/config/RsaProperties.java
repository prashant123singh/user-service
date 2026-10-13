package com.vinsguru.netflux.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "rsa")
public record RsaProperties(Path privateKey) {
}
