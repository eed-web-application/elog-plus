package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Tag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TagRepository extends MongoRepository<Tag, String> {
    boolean existsByName(String name);
}
