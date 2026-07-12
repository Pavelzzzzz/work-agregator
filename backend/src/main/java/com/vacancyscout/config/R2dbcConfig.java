package com.vacancyscout.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

@Configuration
public class R2dbcConfig {

  @Bean
  public R2dbcCustomConversions r2dbcCustomConversions(ObjectMapper objectMapper) {
    return R2dbcCustomConversions.of(
        PostgresDialect.INSTANCE,
        new SkillsWritingConverter(objectMapper),
        new SkillsReadingConverter(objectMapper));
  }

  private static final TypeReference<List<String>> SKILLS_TYPE = new TypeReference<>() {};

  @WritingConverter
  static class SkillsWritingConverter implements Converter<List<String>, Json> {
    private final ObjectMapper objectMapper;

    SkillsWritingConverter(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public Json convert(List<String> source) {
      try {
        return Json.of(objectMapper.writeValueAsString(source));
      } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to serialize skills", e);
      }
    }
  }

  @ReadingConverter
  static class SkillsReadingConverter implements Converter<Json, List<String>> {
    private final ObjectMapper objectMapper;

    SkillsReadingConverter(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public List<String> convert(Json source) {
      if (source == null) {
        return Collections.emptyList();
      }
      try {
        return objectMapper.readValue(source.asString(), SKILLS_TYPE);
      } catch (Exception e) {
        return Collections.emptyList();
      }
    }
  }
}
