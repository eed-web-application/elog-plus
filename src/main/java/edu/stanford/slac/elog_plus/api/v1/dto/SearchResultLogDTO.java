package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Contains the minimal set of information  returned by all the search API")
public record SearchResultLogDTO (
    @Schema(description = "record primary key")
    String id,
    String logbook,
    String priority,
    String segment,
    List<String> tags,
    String title,
    String author,
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime logDate
    ){}
