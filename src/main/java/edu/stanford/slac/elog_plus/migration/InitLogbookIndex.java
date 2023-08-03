package edu.stanford.slac.elog_plus.migration;


import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Logbook;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@AllArgsConstructor
@ChangeUnit(id = "logbook-index", order = "3", author = "bisegni")
public class InitLogbookIndex {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        //entry index
        MongoDDLOps.createIndex(
                Logbook.class,
                mongoTemplate,
                new Index().on(
                        "name",
                        Sort.Direction.ASC
                )
        );
        MongoDDLOps.createIndex(
                Logbook.class,
                mongoTemplate,
                new Index().on(
                        "tags",
                        Sort.Direction.ASC
                )
        );
        MongoDDLOps.createIndex(
                Logbook.class,
                mongoTemplate,
                new Index().on(
                        "shifts",
                        Sort.Direction.ASC
                )
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
