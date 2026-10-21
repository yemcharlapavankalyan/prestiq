package org.prestiq.recommendation.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.prestiq.recommendation.dto.response.ScoreBreakdownDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Converter
public class ScoreBreakdownConverter implements AttributeConverter<ScoreBreakdownDto, String> {

    private static final Logger log = LoggerFactory.getLogger(ScoreBreakdownConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(ScoreBreakdownDto attribute) {
        if (attribute == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error serializing score breakdown to JSON", e);
            return "{}";
        }
    }

    @Override
    public ScoreBreakdownDto convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "{}".equals(dbData.trim())) {
            return null;
        }
        try {
            return objectMapper.readValue(dbData, ScoreBreakdownDto.class);
        } catch (IOException e) {
            log.error("Error deserializing score breakdown from JSON: {}", dbData, e);
            return null;
        }
    }
}
