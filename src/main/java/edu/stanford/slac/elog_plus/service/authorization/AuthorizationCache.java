package edu.stanford.slac.elog_plus.service.authorization;


import edu.stanford.slac.elog_plus.api.v1.dto.LogbookSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

@Component
@Data
@AllArgsConstructor
@RequestScope
public class AuthorizationCache {
    private List<String> authorizedLogbookId;
    private List<LogbookSummaryDTO> authorizedLogbookSummaries;
}
