package edu.stanford.slac.elog_plus.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Is the logbook custom token to authorize third parties application to access logbook data
 */
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Document
public class AuthenticationToken {
    private String id;
    private String name;
    private String email;
    private LocalDate expiration;
    private String token;
}
