package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LogRepository extends MongoRepository<Log, String>, LogRepositoryCustom {
    Page<Log> findByLogbookIn(List<String> logbook, Pageable pageable);

    List<Log> findAllByIdIn(List<String> ids);

    Optional<Log> findBySupersedeBy(String id);

    /**
     * Return the log that the one identified by id is his followUp
     * @param id the id of the followup record
     * @return the following up record
     */
    Optional<Log> findByFollowUpContains(String id);
}
