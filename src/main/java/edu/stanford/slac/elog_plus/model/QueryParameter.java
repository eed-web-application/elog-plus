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
public class QueryParameter {
    @Builder.Default
    private Integer page = 0;
    @Builder.Default
    private Integer rowPerPage = 30;
    @Builder.Default
    List<String> logBook = Collections.emptyList();
    @Builder.Default
    private LocalDateTime from = null;
    @Builder.Default
    private LocalDateTime to = null;

}
