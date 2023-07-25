package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.GregorianCalendar;

/**
 * Represent the shift during the day
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder(toBuilder = true)
public class Shift {
    private String id;
    private String name;
    private String from;
    private String to;
    // calculated fields
    private LocalTime fromTime;
    private LocalTime toTime;
}
