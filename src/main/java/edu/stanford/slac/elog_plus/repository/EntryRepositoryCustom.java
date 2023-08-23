package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;

import java.util.List;

public interface EntryRepositoryCustom {
    List<Entry> searchAll(QueryParameterWithAnchor queryWithAnchorDTO);
    List<String> getAllTags();
    void setSupersededBy(String entryId, String supersededById);

    /**
     * Return all the entry ids that are referenced by the one identified by the id
     * @param id the source entry unique id
     * @return the list of referenced entries
     */
    List<String> findReferencesBySourceId(String id);
}
