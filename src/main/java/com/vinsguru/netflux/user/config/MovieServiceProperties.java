package com.vinsguru.netflux.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service.movie")
public record MovieServiceProperties(String baseUrl) {
}
