package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AuthenticationTokenRepository extends MongoRepository<AuthenticationToken, String> {
    Optional<AuthenticationToken> findByName(String name);

    boolean existsByName(String name);

    Optional<AuthenticationToken> findByEmailIs(String email);
}
