package edu.stanford.slac.elog_plus.ldap_repository;

import edu.stanford.slac.elog_plus.model.Person;
import org.springframework.data.ldap.repository.LdapRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends LdapRepository<Person> {
    Optional<Person> findByMail(String mail);
    List<Person> findByCommonNameContainsIgnoreCaseOrderByCommonNameAsc(String commonNamePrefix);
}