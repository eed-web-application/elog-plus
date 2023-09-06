package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.*;
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
public class Authorization {
    public enum Type{
        Read,
        Write,
        Create,
        Admin,
        Root
    }
    @Id
    private String id;
    /**
     * The authorizationType is a general string value
     * that express the entitlement to take an action
     * on the specific resource
     */
    private Type authorizationType;
    /**
     * Is the type of the resource to which the
     * authorization belong
     * ex: /logbook/id
     * ex: /logbook/id/entry/id
     */
    private String resource;
    /**
     * the value identify the owner of the authorization
     */
    private String owner;

    @CreatedDate
    private LocalDateTime creationDate;
    @CreatedBy
    private String creationBy;
    @LastModifiedDate
    private LocalDateTime modificationDate;
    @LastModifiedBy
    private String modifiedBy;
}
