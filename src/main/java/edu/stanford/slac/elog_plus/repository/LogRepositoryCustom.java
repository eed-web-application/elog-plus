package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elogplus.model.Log;
import edu.stanford.slac.elogplus.model.QueryParameter;
import org.springframework.data.domain.Page;

public interface LogRepositoryCustom {
    Page<Log> searchAll(QueryParameter parameter);
}
