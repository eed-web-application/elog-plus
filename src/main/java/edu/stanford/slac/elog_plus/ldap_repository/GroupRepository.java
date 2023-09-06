package edu.stanford.slac.elog_plus.ldap_repository;

import edu.stanford.slac.elog_plus.model.Group;
import edu.stanford.slac.elog_plus.model.Person;
import org.springframework.data.ldap.repository.LdapRepository;

import java.util.List;

public interface GroupRepository extends LdapRepository<Group> {
    List<Group> findByCommonNameContainsIgnoreCaseOrderByCommonNameAsc(String commonNamePrefix);
}