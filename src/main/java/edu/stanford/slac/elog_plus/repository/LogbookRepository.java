package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository for the logbook managements
 */
public interface LogbookRepository extends MongoRepository<Logbook, String>, LogbookRepositoryCustom{
    boolean existsById(String id);

    boolean existsByName(String name);

    Optional<Logbook> findByName(String logbookName);
}
