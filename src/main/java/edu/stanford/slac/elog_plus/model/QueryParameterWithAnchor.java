package edu.stanford.slac.elog_plus.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QueryParameterWithAnchor {
    @Builder.Default
    LocalDateTime anchorDate = null;
    @Builder.Default
    Integer logsBefore = 0;
    @Builder.Default
    Integer logsAfter = 10;
    @Builder.Default
    String textSearch = null;
    @Builder.Default
    List<String> tags  = Collections.emptyList();
    @Builder.Default
    List<String> logBook = Collections.emptyList();
    @Builder.Default
    private LocalDateTime from = null;
    @Builder.Default
    private LocalDateTime to = null;

}
