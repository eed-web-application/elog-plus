package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Authorization;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the attachment managements
 */
public interface AuthorizationRepository extends MongoRepository<Authorization, String> {

    Optional<Authorization> findByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Authorization.Type authorizationType);

    List<Authorization> findByResourceIsAndAuthorizationTypeIs(String resource, Authorization.Type authorizationType);

    void deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(String owner, String resource, Authorization.Type authorizationType);
}
