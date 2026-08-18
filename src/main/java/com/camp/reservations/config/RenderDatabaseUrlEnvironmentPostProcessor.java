package com.camp.reservations.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Render's managed Postgres exposes connection info as a single
 * {@code postgresql://user:password@host:port/database} URL via the DATABASE_URL
 * env var, but the JDBC driver needs {@code jdbc:postgresql://host:port/database}
 * plus username/password as separate properties. This translates one into the
 * other, and only when DATABASE_URL is actually set — local dev (no such env var)
 * keeps using the H2 defaults in application.properties untouched.
 *
 * <p>Must run <em>after</em> {@link ConfigDataEnvironmentPostProcessor} (which loads
 * application.properties) — otherwise Boot's own config loading re-inserts the H2
 * settings above whatever this adds, silently discarding the override.
 */
public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo() != null ? uri.getUserInfo().split(":", 2) : new String[0];
        String username = userInfo.length > 0 ? userInfo[0] : "";
        String password = userInfo.length > 1 ? userInfo[1] : "";
        int port = uri.getPort() != -1 ? uri.getPort() : 5432;
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbcUrl);
        props.put("spring.datasource.username", username);
        props.put("spring.datasource.password", password);
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

        environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", props));
    }
}
