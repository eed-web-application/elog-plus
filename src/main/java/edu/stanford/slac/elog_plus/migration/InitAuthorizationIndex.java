package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.model.Authorization;
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
@ChangeUnit(id = "init-authorization-index", order = "7", author = "bisegni")
public class InitAuthorizationIndex extends MongoDDLOps {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "resource",
                                Sort.Direction.ASC
                        )
                        .named("resource")
        );
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "authorizationType",
                                Sort.Direction.ASC
                        )
                        .named("authorizationType")
        );
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "owner",
                                Sort.Direction.ASC
                        )
                        .named("owner")
        );
        MongoDDLOps.createIndex(
                Authorization.class,
                mongoTemplate,
                new Index()
                        .on(
                                "owner",
                                Sort.Direction.ASC
                        )
                        .on(
                                "authorizationType",
                                Sort.Direction.ASC
                        )
                        .unique()
                        .sparse()
                        .named("ownerAuthTarget")
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
