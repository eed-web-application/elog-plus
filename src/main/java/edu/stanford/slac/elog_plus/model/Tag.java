package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Tag {
    @Id
    private String id;
    /**
     * The name of the tag
     */
    private String name;
    /**
     * The description of the tag
     * will be used during the selection of the right tag from the AI tools
     */
    private String description;
}
