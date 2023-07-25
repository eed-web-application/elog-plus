package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Shift;
import edu.stanford.slac.elog_plus.model.Tag;

import java.util.List;

public interface LogbookRepositoryCustom {
    String createNewTag(String logbookId, Tag newTag);

    List<Tag> getAllTagFor(String logbookId);

    boolean tagExistByName(String logbookId, String tagName);

    List<Shift> getAllShift(String logbookID);

    List<String> getAllLogbook();


}
