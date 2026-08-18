package com.camp.reservations.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/h2-console/**").permitAll()
                        .requestMatchers("/", "/register", "/login").permitAll()
                        .requestMatchers("/reservations", "/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/campsites/{id:\\d+}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reservations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/campsites", "/api/campsites/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reservations", "/api/reservations/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reservations", "/api/reservations/**").permitAll()
                        .requestMatchers("/my/**").authenticated()
                        .requestMatchers("/campsites/new", "/campsites/*/edit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/campsites", "/campsites/*", "/campsites/*/delete").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/campsites", "/api/campsites/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/campsites/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/campsites/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/my/campsites", false)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        return http.build();
    }
}
