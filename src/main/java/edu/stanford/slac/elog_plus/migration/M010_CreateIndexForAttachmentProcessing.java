package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.model.Attachment;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Log4j2
@AllArgsConstructor
@ChangeUnit(id = "attachment-processing-search-index", order = "10", author = "bisegni")
public class M010_CreateIndexForAttachmentProcessing {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                Attachment.class,
                mongoTemplate,
                new Index()
                        .on(
                                "createdDate",
                                Sort.Direction.ASC
                        )
                        .on(
                                "referenceInfo",
                                Sort.Direction.ASC
                        )
                        .on(
                                "inUse",
                                Sort.Direction.ASC
                        )
                        .on(
                                "processingId",
                                Sort.Direction.ASC
                        )
                        .on(
                                "processingTimestamp",
                                Sort.Direction.ASC
                        )
                        .on(
                                "canBeDeleted",
                                Sort.Direction.ASC
                        )
                        .named("findAndUpdateNextAvailable")
        );
    }

    @RollbackExecution
    public void rollback() {
    }

}