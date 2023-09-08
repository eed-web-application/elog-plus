package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Authorization;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the attachment managements
 */
public interface AuthorizationRepository extends MongoRepository<Authorization, String> {
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(String owner, String resource, Integer authorizationType);
    List<Authorization> findByResourceIs(String resource);
    List<Authorization> findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(String resource, Integer authorizationType);
    List<Authorization> findByOwnerAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(String owner, Integer type, String resource);
    void deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    void deleteAllByResourceStartingWith(String resourcePrefix);
    void deleteAllByResourceIs(String resource);
}
