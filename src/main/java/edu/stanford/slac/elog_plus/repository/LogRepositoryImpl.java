package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.model.QueryParameter;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@AllArgsConstructor
public class LogRepositoryImpl implements LogRepositoryCustom {
    final private MongoTemplate mongoTemplate;

    @Override
    public Page<Log> searchAll(QueryParameter parameter) {
        Pageable pageable = PageRequest.of(
                parameter.getPage(),
                parameter.getRowPerPage()
        );
        List<Criteria> allCriteria = new ArrayList<>();
        if (!parameter.getLogBook().isEmpty()) {
            allCriteria.add(
                    Criteria.where("LOGBOOK").in(
                            parameter.getLogBook()
                    )
            );
        }
        Query query = new Query()
                .with(Sort.by(Sort.Direction.DESC, "LOGDATE"))
                .with(
                        pageable
                );
        query.addCriteria(Criteria.where("supersedeBy").exists(false));
        if (allCriteria.size() > 0) {
            query.addCriteria(new Criteria().orOperator(
                            allCriteria
                    )
            );
        }
        List<Log> found = mongoTemplate.find(query, Log.class);
        return PageableExecutionUtils.getPage(
                found,
                pageable,
                () -> mongoTemplate.count(
                        Query.of(query).limit(-1).skip(-1), Log.class)
        );
    }

    @Override
    public List<Log> searchAll(QueryParameterWithAnchor queryWithAnchor) {
        if (queryWithAnchor.getLogsBefore() != null && queryWithAnchor.getLogsAfter() == null) {
            throw ControllerLogicException.of(
                    -1,
                    "logs before count cannot be used without an anchor id",
                    "LogRepositoryImpl::searchUsingAnchor"
            );
        }
        if (queryWithAnchor.getLogsAfter() == null) {
            throw ControllerLogicException.of(
                    -2,
                    "the logs after count is mandatory",
                    "LogRepositoryImpl::searchUsingAnchor"
            );
        }
        List<Criteria> allCriteria = new ArrayList<>();
        if (!queryWithAnchor.getLogBook().isEmpty()) {
            allCriteria.add(
                    Criteria.where("LOGBOOK").in(
                            queryWithAnchor.getLogBook()
                    )
            );
        }
        // supersede criteria
        allCriteria.add(
                Criteria.where("supersedeBy").exists(false)
        );

        List<Log> logsBeforeAnchor = new ArrayList<>();
        List<Log> logsAfterAnchor = new ArrayList<>();

        if (
                queryWithAnchor.getLogsBefore() != null
                        && queryWithAnchor.getLogsBefore() > 0
                        && queryWithAnchor.getAnchorDate() != null
        ) {
            Query q = new Query();
            q.addCriteria(new Criteria().andOperator(
                            allCriteria
                    )
            ).addCriteria(
                    Criteria.where("LOGDATE")
                            .gte(queryWithAnchor.getAnchorDate())
            ).with(
                    Sort.by(
                            Sort.Direction.DESC, "LOGDATE")
            ).limit(queryWithAnchor.getLogsBefore());
            logsBeforeAnchor.addAll(mongoTemplate.find(
                            q,
                            Log.class
                    )
            );
        }

        if (queryWithAnchor.getLogsAfter() != null && queryWithAnchor.getLogsAfter() > 0) {
            Query q = new Query();
            if (queryWithAnchor.getAnchorDate() != null) {
                q.addCriteria(
                        Criteria.where("LOGDATE").lt(queryWithAnchor.getAnchorDate())
                );
            }
            q.addCriteria(new Criteria().andOperator(
                            allCriteria
                    )
            ).with(
                    Sort.by(
                            Sort.Direction.DESC, "LOGDATE")
            ).limit(queryWithAnchor.getLogsAfter());
            logsAfterAnchor = mongoTemplate.find(
                    q,
                    Log.class
            );
        }
        logsBeforeAnchor.addAll(logsAfterAnchor);
        return logsBeforeAnchor;
    }

    @Override
    public List<String> getAllTags() {
        return mongoTemplate.findDistinct(new Query(), "tags", Log.class, String.class);
    }
}
