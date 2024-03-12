package edu.stanford.slac.elog_plus.model;


import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Logbook {
    @Id
    private String id;
    private String name;
    @Builder.Default
    private List<Tag> tags = Collections.emptyList();
    @Builder.Default
    private List<Shift> shifts = Collections.emptyList();
}
