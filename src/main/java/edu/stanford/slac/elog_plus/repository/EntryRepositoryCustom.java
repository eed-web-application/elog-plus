package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;

import java.util.List;

public interface EntryRepositoryCustom {
    List<Entry> searchAll(QueryParameterWithAnchor queryWithAnchorDTO);
    List<String> getAllTags();
}
