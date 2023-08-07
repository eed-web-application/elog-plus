package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document()
public class Logbook {
    @Id
    private String id;
    @Indexed(unique = true)
    private String name;
    @Indexed
    @Builder.Default
    private List<Tag> tags = Collections.emptyList();
    @Indexed
    @Builder.Default
    private List<String> tagNames = Collections.emptyList();
    @Indexed
    @Builder.Default
    private List<Shift> shifts = Collections.emptyList();
}
