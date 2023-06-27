package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.model.QueryParameter;
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

        if (allCriteria.size() > 0) {
            query.addCriteria(new Criteria().orOperator
                    (
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
}
