package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;

@Repository
@AllArgsConstructor
public class EntryRepositoryImpl implements EntryRepositoryCustom {
    final private MongoTemplate mongoTemplate;

    private Entry getEntryByIDWithOnlyDate(String id) {
        Entry result = null;
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        ).fields().include("eventAt", "loggedAt");
        return mongoTemplate.findOne(q, Entry.class);
    }

    @Override
    public List<Entry> searchAll(QueryParameterWithAnchor queryWithAnchor) {
        if (queryWithAnchor.getContextSize() != null && queryWithAnchor.getLimit() == null) {
            throw ControllerLogicException.of(
                    -1,
                    "logs before count cannot be used without an anchor id",
                    "LogRepositoryImpl::searchUsingAnchor"
            );
        }
        if (queryWithAnchor.getLimit() == null) {
            throw ControllerLogicException.of(
                    -2,
                    "the logs after count is mandatory",
                    "LogRepositoryImpl::searchUsingAnchor"
            );
        }

        Entry anchorEntry = null;
        if(queryWithAnchor.getAnchorID() !=null) {
            anchorEntry = getEntryByIDWithOnlyDate(queryWithAnchor.getAnchorID());
        }

        List<Criteria> allCriteria = new ArrayList<>();
        if (!queryWithAnchor.getLogbooks().isEmpty()) {
            allCriteria.add(
                    Criteria.where("logbooks").in(
                            queryWithAnchor.getLogbooks()
                    )
            );
        }

        if (!queryWithAnchor.getTags().isEmpty()) {
            allCriteria.add(
                    Criteria.where("tags").in(
                            queryWithAnchor.getTags()
                    )
            );
        }
        if (queryWithAnchor.getHideSummaries()!= null && queryWithAnchor.getHideSummaries()) {
            allCriteria.add(
                    Criteria.where("summarizes").exists(false)
            );
        }
        // supersede criteria
        allCriteria.add(
                Criteria.where("supersedeBy").exists(false)
        );

        List<Entry> logsBeforeAnchor = new ArrayList<>();
        List<Entry> logsAfterAnchor = new ArrayList<>();

        if (
                queryWithAnchor.getContextSize() != null
                        && queryWithAnchor.getContextSize() > 0
                        && queryWithAnchor.getAnchorID() != null
        ) {
            List<Criteria> localAllCriteria = allCriteria;
            Query q = getDefaultQuery(queryWithAnchor.getSearch());
            q.addCriteria(
                    Criteria.where(
                            getSortedField(queryWithAnchor)
                    ).gte(
                            getAnchorValueDate(queryWithAnchor, anchorEntry)
                    )
            );
            applyDateCriteriaForContextEntries(localAllCriteria, queryWithAnchor);
            q.addCriteria(
                    // all general criteria
                    new Criteria().andOperator(
                            localAllCriteria
                    )

            ).with(
                    Sort.by(
                            Sort.Direction.ASC, getSortedField(queryWithAnchor))
            ).limit(queryWithAnchor.getContextSize());
            logsBeforeAnchor.addAll(mongoTemplate.find(
                            q,
                            Entry.class
                    )
            );
            Collections.reverse(logsBeforeAnchor);
        }

        if (queryWithAnchor.getLimit() != null && queryWithAnchor.getLimit() > 0) {
            List<Criteria> localAllCriteria = allCriteria;
            Query q = getDefaultQuery(queryWithAnchor.getSearch());
            applyDateCriteriaForLimitEntries(localAllCriteria, queryWithAnchor);
            if(anchorEntry != null) {
                q.addCriteria(
                        Criteria.where(
                                getSortedField(queryWithAnchor)
                        ).lt(
                                getAnchorValueDate(queryWithAnchor, anchorEntry)
                        )
                );
            }
            q.addCriteria(new Criteria().andOperator(
                    localAllCriteria
                    )
            ).with(
                    Sort.by(
                            Sort.Direction.DESC, getSortedField(queryWithAnchor))
            ).limit(queryWithAnchor.getLimit());
            logsAfterAnchor = mongoTemplate.find(
                    q,
                    Entry.class
            );
        }

        logsBeforeAnchor.addAll(logsAfterAnchor);
        return logsBeforeAnchor;
    }

    @Override
    public List<String> getAllTags() {
        return mongoTemplate.findDistinct(new Query(), "tags", Entry.class, String.class);
    }

    private Query getDefaultQuery(String textSearch) {
        if (textSearch != null && !textSearch.isEmpty()) {
            //{$text: {$search:'log' }}
            return TextQuery.queryText(TextCriteria.forDefaultLanguage()
                    .matchingAny(textSearch.split(" "))
            );
        } else {
            return new Query();
        }
    }

    private void applyDateCriteriaForContextEntries(List<Criteria> allCriteria, QueryParameterWithAnchor queryWithAnchor) {
        if(queryWithAnchor.getEndDate() != null) {
            allCriteria.add(
                    Criteria.where(getSortedField(queryWithAnchor))
                            .gte(queryWithAnchor.getEndDate())
            );
        }
    }

    private void applyDateCriteriaForLimitEntries(List<Criteria> allCriteria, QueryParameterWithAnchor queryWithAnchor) {
        if (queryWithAnchor.getEndDate() != null) {
            allCriteria.add(
                    Criteria.where(getSortedField(queryWithAnchor)).lte(queryWithAnchor.getEndDate())
            );
        }
        if (queryWithAnchor.getStartDate() != null) {
            allCriteria.add(
                    Criteria.where(getSortedField(queryWithAnchor)).gte(queryWithAnchor.getStartDate())
            );
        }


    }

    private String getSortedField(QueryParameterWithAnchor queryWithAnchor) {
        return (queryWithAnchor.getSortByLogDate() != null && queryWithAnchor.getSortByLogDate()) ? "loggedAt" : "eventAt";
    }

    private LocalDateTime getAnchorValueDate(QueryParameterWithAnchor queryWithAnchor, Entry anchorEntry) {
        return (queryWithAnchor.getSortByLogDate() != null && queryWithAnchor.getSortByLogDate()) ?
                anchorEntry.getLoggedAt() : anchorEntry.getEventAt();
    }
}
