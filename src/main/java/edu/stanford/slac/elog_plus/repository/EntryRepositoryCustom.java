package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.QueryParameter;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;
import org.springframework.data.domain.Page;

import java.util.List;

public interface EntryRepositoryCustom {
    Page<Entry> searchAll(QueryParameter parameter);
    List<Entry> searchAll(QueryParameterWithAnchor queryWithAnchorDTO);
    List<String> getAllTags();
}
