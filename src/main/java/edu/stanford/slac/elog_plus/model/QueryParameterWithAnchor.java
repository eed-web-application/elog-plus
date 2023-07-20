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
    LocalDateTime startDate = null;
    @Builder.Default
    LocalDateTime endDate = null;
    @Builder.Default
    Integer contextSize = 0;
    @Builder.Default
    Integer limit = 0;
    @Builder.Default
    String search = null;
    @Builder.Default
    List<String> tags  = Collections.emptyList();
    @Builder.Default
    List<String> logbooks = Collections.emptyList();
    @Builder.Default
    private LocalDateTime from = null;
    @Builder.Default
    private LocalDateTime to = null;

}
