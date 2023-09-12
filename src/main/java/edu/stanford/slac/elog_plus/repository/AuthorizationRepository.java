package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Authorization;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the attachment managements
 */
public interface AuthorizationRepository extends MongoRepository<Authorization, String> {
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    @Query("{ 'owner' : '?0', $or: [{'resource' : '?1', 'authorizationType' : { '$gte' : ?2}},{'authorizationType' : 2, resource:'*'}]}")
    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(String owner, String resource, Integer authorizationType);
    List<Authorization> findByResourceIs(String resource);
    List<Authorization> findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(String resource, Integer authorizationType);
    //@Query("{ 'owner' : '?0', $or: [{'authorizationType' : { '$gte' : '?1'}, 'resource' : { $regex : \"?2\"}}, {'authorizationType' : 2, resource:'*'}]}")
    List<Authorization> findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(String owner, Authorization.OType ownerType, Integer authorizationType, String resource);
    void deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Integer authorizationType);
    void deleteAllByResourceStartingWith(String resourcePrefix);
    void deleteAllByResourceIs(String resource);
}
