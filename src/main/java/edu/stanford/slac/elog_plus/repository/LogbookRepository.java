package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.model.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the logbooks managements
 */
public interface LogbookRepository extends MongoRepository<Logbook, String>, LogbookRepositoryCustom{
    boolean existsById(String id);

    boolean existsByName(String name);

    Optional<Logbook> findByName(String logbookName);

    boolean existsByIdInAndTagsIdIs(List<String> logbookIds, String tagId);
}
