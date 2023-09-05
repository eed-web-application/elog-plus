package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Person;
import org.springframework.data.ldap.repository.LdapRepository;

import java.util.List;

public interface PersonRepository extends LdapRepository<Person> {
    List<Person> findByCommonNameLikeIgnoreCase(String commonName);
}