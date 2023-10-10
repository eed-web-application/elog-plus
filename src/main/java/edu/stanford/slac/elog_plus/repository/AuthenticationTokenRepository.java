package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AuthenticationTokenRepository extends MongoRepository<AuthenticationToken, String> {
    //return all token managed by the application at startup time
    List<AuthenticationToken> findAllByApplicationManagedIsTrue();

    void deleteAllByApplicationManagedIsTrue();

    Optional<AuthenticationToken> findByName(String name);

    boolean existsByName(String name);

    Optional<AuthenticationToken> findByEmailIs(String email);

    Optional<AuthenticationToken> findByNameIsAndEmailEndsWith(String name, String emailPostfix);

    List<AuthenticationToken> findAllByEmailEndsWith(String emailPostfix);

    boolean existsByEmail(String email);

    void deleteAllByEmailEndsWith(String emailPostfix);
}
