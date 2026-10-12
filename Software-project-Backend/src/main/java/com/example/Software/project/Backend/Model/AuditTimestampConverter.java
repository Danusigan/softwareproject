package com.example.Software.project.Backend.Model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** Audit values are UTC, regardless of the JVM timezone used by Timestamp.valueOf(LocalDateTime). */
@Converter
public class AuditTimestampConverter implements AttributeConverter<LocalDateTime, Timestamp> {
    @Override public Timestamp convertToDatabaseColumn(LocalDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant(ZoneOffset.UTC));
    }
    @Override public LocalDateTime convertToEntityAttribute(Timestamp value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }
}
