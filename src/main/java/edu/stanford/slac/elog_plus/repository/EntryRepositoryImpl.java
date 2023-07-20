package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@AllArgsConstructor
public class EntryRepositoryImpl implements EntryRepositoryCustom {
    final private MongoTemplate mongoTemplate;

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
        List<Criteria> allCriteria = new ArrayList<>();
        if (!queryWithAnchor.getLogbooks().isEmpty()) {
            allCriteria.add(
                    Criteria.where("logbook").in(
                            queryWithAnchor.getLogbooks()
                    )
            );
        }

        if(!queryWithAnchor.getTags().isEmpty()) {
            allCriteria.add(
                    Criteria.where("tags").in(
                            queryWithAnchor.getTags()
                    )
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
                        && queryWithAnchor.getStartDate() != null
        ) {
            Query q = getDefaultQuery(queryWithAnchor.getSearch());
            q.addCriteria(new Criteria().andOperator(
                            allCriteria
                    )
            ).addCriteria(
                    Criteria.where("loggedAt")
                            .gte(queryWithAnchor.getStartDate())
            ).with(
                    Sort.by(
                            Sort.Direction.ASC, "loggedAt")
            ).limit(queryWithAnchor.getContextSize());
            logsBeforeAnchor.addAll(mongoTemplate.find(
                            q,
                            Entry.class
                    )
            );
            Collections.reverse(logsBeforeAnchor);
        }

        if (queryWithAnchor.getLimit() != null && queryWithAnchor.getLimit() > 0) {
            Query q = getDefaultQuery(queryWithAnchor.getSearch());
            if (queryWithAnchor.getStartDate() != null) {
                q.addCriteria(
                        Criteria.where("loggedAt").lt(queryWithAnchor.getStartDate())
                );
            }
            q.addCriteria(new Criteria().andOperator(
                            allCriteria
                    )
            ).with(
                    Sort.by(
                            Sort.Direction.DESC, "loggedAt")
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
        if(textSearch!=null && !textSearch.isEmpty()) {
            //{$text: {$search:'log' }}
            return TextQuery.queryText(TextCriteria.forDefaultLanguage()
                    .matchingAny(textSearch.split(" "))
            );
        } else {
            return new Query();
        }
    }
}
