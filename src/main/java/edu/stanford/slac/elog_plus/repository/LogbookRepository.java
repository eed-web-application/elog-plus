package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for the logbook managements
 */
public interface LogbookRepository extends MongoRepository<Logbook, String> {
    boolean existsByName(String name);
}
