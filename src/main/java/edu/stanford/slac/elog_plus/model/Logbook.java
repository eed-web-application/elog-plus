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
    /**
     * The name of the logbook
     */
    private String name;
    /**
     * If true, all users can read the logbook
     */
    @Builder.Default
    private boolean readAll = false;
    /**
     * If true, all users can write the logbook
     */
    @Builder.Default
    private boolean writeAll = false;
    /**
     * Are all the tag that can be used in this logbook
     */
    @Builder.Default
    private List<Tag> tags = Collections.emptyList();
    /**
     * Are all the shifts that can be used in this logbook
     */
    @Builder.Default
    private List<Shift> shifts = Collections.emptyList();
}
