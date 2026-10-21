package org.prestiq.eligibility.domain.entity;

public enum CriterionOperator {
    // Numeric operators
    GTE,
    LTE,
    GT,
    LT,
    EQ,
    BETWEEN,

    // Set membership operators
    IN,
    NOT_IN,
    CONTAINS_ANY,

    // Boolean operators
    IS_TRUE,
    IS_FALSE,

    // String operators
    EQUALS,
    NOT_EQUALS;

    public static CriterionOperator fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Operator cannot be null");
        }
        try {
            return CriterionOperator.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported criterion operator: '" + value + "'");
        }
    }
}
