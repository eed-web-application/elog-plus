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
     * @param summarizesShiftId the shift name
     * @param summarizesDate the date
     * @return the found summary, if any
     */
    @Query(fields = "{ 'summarizes' : 1}")
    Optional<Entry> findBySummarizes_ShiftIdAndSummarizes_Date(String summarizesShiftId, LocalDate summarizesDate);

    /**
     * Return the number of the summary associated to a shift
     * @param summarizesShiftId the id of the shift
     * @return the number of summaries associated to the shift
     */
    long countBySummarizes_ShiftId(String summarizesShiftId);

    /**
     * Return the number of entries that are associated to a specific tag
     * @param tagName is the tag name
     * @return the number of entries that are associated to the tag
     */
    long countByTagsContains(String tagName);

    /**
     * Check if an entry exists using and origin id
     * @param originId the id from the original system
     * @return true if the entry exists
     */
    boolean existsByOriginId(String originId);
}
