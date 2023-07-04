package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface LogRepository extends MongoRepository<Log, String>, LogRepositoryCustom {
    Page<Log> findByLogbookIn(List<String> logbook, Pageable pageable);

    List<Log> findAllByIdIn(List<String> ids);
}
