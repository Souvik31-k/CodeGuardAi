package com.codeguard.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads the .env file from the classpath and registers its entries as a
 * PropertySource in the Spring Environment. This makes ${ENCRYPTION_KEY}
 * (and any other env variables) available for @Value placeholders.
 */
@Configuration
public class DotenvConfig {

    private final ConfigurableEnvironment env;

    public DotenvConfig(ConfigurableEnvironment env) {
        this.env = env;
    }

    @PostConstruct
    public void loadDotenv() {
        // Load .env from the classpath root (src/main/resources)
        Dotenv dotenv = Dotenv.configure()
                .directory("./src/main/resources")
                .ignoreIfMissing()
                .load();

        Map<String, Object> map = new HashMap<>();
        dotenv.entries().forEach(entry -> map.put(entry.getKey(), entry.getValue()));

        // Register a high‑precedence property source so that placeholders resolve.
        env.getPropertySources().addFirst(new MapPropertySource("dotenv", map));
    }
}
