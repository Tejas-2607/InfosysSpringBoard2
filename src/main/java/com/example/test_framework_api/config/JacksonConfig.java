package com.example.test_framework_api.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
// import com.fasterxml.jackson.databind.ObjectMapper;


@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder objectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.modules(new JavaTimeModule()); // Registers support for LocalDateTime
        return builder;
    }

    // @Bean
    // @Override
    // public ObjectMapper objectMapper() {
    //     ObjectMapper mapper = super.objectMapper();
    //     mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    //     mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    //     return mapper;
    // }
}