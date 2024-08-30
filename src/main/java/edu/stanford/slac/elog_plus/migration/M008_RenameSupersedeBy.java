package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.model.Entry;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Log4j2
@AllArgsConstructor
@ChangeUnit(id = "rename-supersededBy", order = "8", author = "bisegni")
public class M008_RenameSupersedeBy {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        //entry index
        Query q = new Query();
        q.addCriteria(
                Criteria.where("supersedeBy").exists(true)
        );

        Update u = new Update();
        u.rename("supersedeBy", "supersededBy");
        log.info("[rename supersedeBy]  start renaming");
        var updateResult = mongoTemplate.updateMulti(q, u, Entry.class);
        log.info("[rename supersedeBy] updated entries: {}", updateResult.getModifiedCount());
    }

    @RollbackExecution
    public void rollback() {}

}