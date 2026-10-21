package org.prestiq.eligibility.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.prestiq.eligibility.dto.response.CriterionResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Converter
public class CriteriaBreakdownConverter implements AttributeConverter<List<CriterionResultResponse>, String> {

    private static final Logger log = LoggerFactory.getLogger(CriteriaBreakdownConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<CriterionResultResponse>> LIST_TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<CriterionResultResponse> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.error("Error serializing criteria breakdown to JSON", e);
            return "[]";
        }
    }

    @Override
    public List<CriterionResultResponse> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, LIST_TYPE);
        } catch (IOException e) {
            log.error("Error deserializing criteria breakdown from JSON: {}", dbData, e);
            return new ArrayList<>();
        }
    }
}
