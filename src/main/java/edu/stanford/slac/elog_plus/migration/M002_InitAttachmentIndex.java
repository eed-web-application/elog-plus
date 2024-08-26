package edu.stanford.slac.elog_plus.migration;


import edu.stanford.slac.elog_plus.model.Attachment;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@AllArgsConstructor
@ChangeUnit(id = "attachment-index", order = "2", author = "bisegni")
public class M002_InitAttachmentIndex {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        //entry index
        MongoDDLOps.createIndex(
                Attachment.class,
                mongoTemplate,
                new Index().on(
                        "fileName",
                        Sort.Direction.ASC
                )
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
