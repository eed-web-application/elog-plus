package edu.stanford.slac.elog_plus.model;

import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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
public class Logbook {
    @Id
    private String id;
    private String name;
    @Builder.Default
    private List<Tag> tags = Collections.emptyList();
    @Builder.Default
    private List<Shift> shifts = Collections.emptyList();
    @Builder.Default
    private List<AuthenticationToken> authenticationTokens = Collections.emptyList();
}
