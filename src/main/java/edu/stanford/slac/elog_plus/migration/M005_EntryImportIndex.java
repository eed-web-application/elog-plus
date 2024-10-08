package edu.stanford.slac.elog_plus.migration;


import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@AllArgsConstructor
@ChangeUnit(id = "entry-import-index", order = "5", author = "bisegni")
public class M005_EntryImportIndex {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        ensureIndex();

    }



    @RollbackExecution
    public void rollback() {

    }

    /**
     * Ensure base index
     */
    private void ensureIndex() {
        var ownerIndex =  MongoDDLOps.checkForIndex(
                Authorization.class,
                mongoTemplate,
                "ownerId"
        );
        if(ownerIndex.isEmpty()) {
            //entry index
            MongoDDLOps.createIndex(
                    Authorization.class,
                    mongoTemplate,
                    new Index().on(
                                    "ownerId",
                                    Sort.Direction.ASC
                            )
                            .named("ownerId")
                            .sparse()
            );
        }
    }
}
