package edu.stanford.slac.elog_plus.migration;


import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Tag;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@AllArgsConstructor
@ChangeUnit(id = "database-initializer", order = "1", author = "bisegni")
public class InitIndex {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(Entry.class, mongoTemplate, mongoMappingContext);
        MongoDDLOps.createIndex(Attachment.class, mongoTemplate, mongoMappingContext);
        MongoDDLOps.createIndex(Tag.class, mongoTemplate, mongoMappingContext);
    }

    @RollbackExecution
    public void rollback() {

    }
}
