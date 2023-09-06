package edu.stanford.slac.elog_plus.migration;


import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Entry;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@AllArgsConstructor
@ChangeUnit(id = "entry-import-index", order = "5", author = "bisegni")
public class EntryImportIndex {
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
                "owner"
        );
        if(ownerIndex.isEmpty()) {
            //entry index
            MongoDDLOps.createIndex(
                    Authorization.class,
                    mongoTemplate,
                    new Index().on(
                                    "owner",
                                    Sort.Direction.ASC
                            )
                            .named("owner")
                            .sparse()
            );
        }
    }
}
