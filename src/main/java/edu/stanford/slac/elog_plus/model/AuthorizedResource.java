package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Define the authorization level on a specific resource
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document()
public class AuthorizedResource {
    private String id;
    private String resourceType;
    @CreatedDate
    private LocalDateTime creationDate;
    @LastModifiedDate
    private LocalDateTime modificationDate;
}
