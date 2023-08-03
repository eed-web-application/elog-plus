package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Entry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EntryRepository extends MongoRepository<Entry, String>, EntryRepositoryCustom {
    Page<Entry> findByLogbookIn(List<String> logbook, Pageable pageable);

    List<Entry> findAllByIdIn(List<String> ids);

    Optional<Entry> findBySupersedeBy(String id);

    /**
     * Return the log that the one identified by id is his followUps
     * @param id the id of the followup record
     * @return the following up record
     */
    Optional<Entry> findByFollowUpsContains(String id);

    /**
     * Return the summary associated to the shift and date
     * @param shift the shift name
     * @param date the date
     * @return
     */
    @Query(fields = "{ 'summarizes' : 1}")
    Optional<Entry> findBySummarizes_ShiftAndSummarizes_Date(String summarizesShift, LocalDate summarizesDate);
}
