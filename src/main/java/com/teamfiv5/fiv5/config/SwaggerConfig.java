package com.teamfiv5.fiv5.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(servers = {
        @Server(url = "https://api-dev.fiv5.app", description = "Development Server (HTTPS)"),
        @Server(url = "http://localhost:8080", description = "Local Development Server (HTTP)")
})
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // API 정보
        Info info = new Info()
                .title("Fiv5 API Document")
                .version("v1.0.0")
                .description("Fiv5 프로젝트 API 명세서");

        // JWT 인증 스키마 설정
        String jwtSchemeName = "jwtAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer")
                        .bearerFormat("JWT")); // Bearer 토큰 포맷

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}