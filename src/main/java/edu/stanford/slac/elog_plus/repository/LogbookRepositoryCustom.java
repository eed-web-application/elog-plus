package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Shift;
import edu.stanford.slac.elog_plus.model.Tag;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface LogbookRepositoryCustom {
    String createNewTag(String logbookId, Tag newTag);

    List<Tag> getAllTagFor(String logbookId);

    boolean tagExistByName(String logbookId, String tagName);

    List<Shift> getAllShift(String logbookID);

    List<String> getAllLogbook();

    Optional<Shift> findShiftFromLocalTime(String logbookId, LocalTime localTime);

    Optional<Shift> findShiftFromLocalTimeWithLogbookName(String logbookId, LocalTime localTime);
}
