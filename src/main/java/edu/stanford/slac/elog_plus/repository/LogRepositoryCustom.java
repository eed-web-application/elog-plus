package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.model.QueryParameter;
import org.springframework.data.domain.Page;

public interface LogRepositoryCustom {
    Page<Log> searchAll(QueryParameter parameter);
}
