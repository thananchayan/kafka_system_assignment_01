package com.thana.kafka_assignment_01.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()

        .info(new Info()
            .title("Kafka Order Processing API")
            .version("v1.0.0")
            .description("Kafka Order Processing API")
            .contact(new Contact()
                .name("Thananchayan EG/2020/4227")
                .email("thananchayan26@gmail.com")
            )
        );
  }

}
