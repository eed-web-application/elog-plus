package edu.stanford.slac.elog_plus.migration;


import com.mongodb.MongoCommandException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.Optional;

public class MongoDDLOps {
    public static <T> void createIndex(Class<T> clazz, MongoTemplate mongoTemplate, MongoMappingContext mongoMappingContext) {
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
        final IndexOperations indexFacility = mongoTemplate.indexOps(clazz);
        resolver.resolveIndexFor(clazz).forEach(i->applyIndex(indexFacility, i));
    }

    public static <T> void createIndex(Class<T> clazz, MongoTemplate mongoTemplate, IndexDefinition definition) {
        final IndexOperations indexFacility = mongoTemplate.indexOps(clazz);
        applyIndex(indexFacility, definition);
    }

    public static <T> Optional<IndexInfo> checkForIndex(Class<T> clazz, MongoTemplate mongoTemplate, String indexName) {
        final IndexOperations indexOps = mongoTemplate.indexOps(clazz);
        return indexOps.getIndexInfo().stream().filter(
                index->index.getName().compareTo(indexName) == 0
        ).findFirst();
    }

    protected static void applyIndex(IndexOperations io, IndexDefinition id) {
        try{
            io.ensureIndex(id);
        } catch (Exception e) {
            if(e.getCause() instanceof MongoCommandException &&
                    (((MongoCommandException)e.getCause()).getErrorCode() == 86 ||
                            ((MongoCommandException)e.getCause()).getErrorCode() == 85)) {
                String indexName = id.getIndexOptions().getString("name");
                io.dropIndex(indexName);
                io.ensureIndex(id);
            } else {
                throw e;
            }
        }
    }

}
