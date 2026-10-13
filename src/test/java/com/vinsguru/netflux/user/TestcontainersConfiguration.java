package com.vinsguru.netflux.user;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	private static final Path PRIVATE_KEY_PATH = Path.of("target", "test-keys", "private-key.pem");

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
	}

	@Bean
	KeyPair testRsaKeyPair() throws NoSuchAlgorithmException {
		var generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	@Bean
	DynamicPropertyRegistrar rsaPrivateKeyRegistrar(KeyPair testRsaKeyPair) {
		writePrivateKeyPem(testRsaKeyPair);
		return registry -> registry.add("rsa.private-key", () -> PRIVATE_KEY_PATH.toAbsolutePath().toString());
	}

	private static void writePrivateKeyPem(KeyPair keyPair) {
		var encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));
		var pem = "-----BEGIN PRIVATE KEY-----\n"
				+ encoder.encodeToString(keyPair.getPrivate().getEncoded())
				+ "\n-----END PRIVATE KEY-----\n";
		try {
			Files.createDirectories(PRIVATE_KEY_PATH.getParent());
			Files.writeString(PRIVATE_KEY_PATH, pem);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
