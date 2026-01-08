package com.teamloci.loci.global.config;

import com.teamloci.loci.global.auth.CustomAuthenticationEntryPoint;
import com.teamloci.loci.global.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final Environment env;

    @Value("${swagger.user:admin}")
    private String swaggerUser;

    @Value("${swagger.password:1234}")
    private String swaggerPassword;

    private static final String[] SWAGGER_URL_PATTERNS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.builder()
                .username(swaggerUser)
                .password(passwordEncoder().encode(swaggerPassword))
                .roles("SWAGGER")
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        boolean isLocal = Arrays.asList(env.getActiveProfiles()).contains("local");

        http
                .securityMatcher(SWAGGER_URL_PATTERNS)
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    if (isLocal) {
                        auth.anyRequest().permitAll();
                    } else {
                        auth.anyRequest().authenticated();
                    }
                });

        if (!isLocal) {
            http.httpBasic(withDefaults());
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "https://api-dev.loci.my"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/health",
                                "/api/v1/auth/**",
                                "/login_test.html",
                                "/api/v1/users/check-handle"
                        ).permitAll()
                        .anyRequest().authenticated());
        http
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                );

        return http.build();
    }
}