package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.model.QueryParameter;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;
import org.springframework.data.domain.Page;

import java.util.List;

public interface LogRepositoryCustom {
    Page<Log> searchAll(QueryParameter parameter);
    List<Log> searchAll(QueryParameterWithAnchor queryWithAnchorDTO);
    List<String> getAllTags();
}
