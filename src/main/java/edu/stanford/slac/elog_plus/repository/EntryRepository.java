package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Entry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface EntryRepository extends MongoRepository<Entry, String>, EntryRepositoryCustom {
    Page<Entry> findByLogbookIn(List<String> logbook, Pageable pageable);

    List<Entry> findAllByIdIn(List<String> ids);

    Optional<Entry> findBySupersedeBy(String id);

    /**
     * Return the log that the one identified by id is his followUp
     * @param id the id of the followup record
     * @return the following up record
     */
    Optional<Entry> findByFollowUpContains(String id);
}
