package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
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
@ChangeUnit(id = "attachment-reference-info-index", order = "9", author = "bisegni")
public class M009_CreateAttachmentReferenceInfoIndex {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                Attachment.class,
                mongoTemplate,
                new Index().on(
                        "referenceInfo",
                        Sort.Direction.ASC
                )
        );
    }

    @RollbackExecution
    public void rollback() {}

}