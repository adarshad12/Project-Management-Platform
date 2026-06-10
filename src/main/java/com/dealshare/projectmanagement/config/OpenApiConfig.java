package com.dealshare.projectmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI projectManagementOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Project Management Platform API")
                        .description("Backend APIs for Jira-like project and sprint management.")
                        .version("v1")
                        .license(new License().name("Internal")));
    }
}
