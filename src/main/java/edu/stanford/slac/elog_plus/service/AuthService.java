package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthMapper;
import edu.stanford.slac.elog_plus.auth.SLACUserInfo;
import edu.stanford.slac.elog_plus.exception.NotAuthenticated;
import edu.stanford.slac.elog_plus.exception.UserNotFound;
import edu.stanford.slac.elog_plus.ldap_repository.GroupRepository;
import edu.stanford.slac.elog_plus.model.Group;
import edu.stanford.slac.elog_plus.model.Person;
import edu.stanford.slac.elog_plus.ldap_repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;

@Service
@AllArgsConstructor
public class AuthService {
    private final AuthMapper authMapper;
    private final PersonRepository personRepository;
    private final GroupRepository groupRepository;

    public PersonDTO findPerson(Authentication authentication) {
        checkAuthentication(authentication, -1);
        return personRepository.findByMail(
                authentication.getCredentials().toString()
        ).map(
                authMapper::fromModel
        ).orElseThrow(
                ()-> UserNotFound.userNotFound()
                        .errorCode(-2)
                        .errorDomain("AuthService::findPerson")
                        .build()
        );
    }

    public List<PersonDTO> findPersons(String searchString, Authentication authentication) throws UsernameNotFoundException {
        checkAuthentication(authentication, -1);
        List<Person> foundPerson = personRepository.findByGecosContainsIgnoreCaseOrderByCommonNameAsc(
                searchString
        );
        return foundPerson.stream().map(
                authMapper::fromModel
        ).toList();
    }

    public List<GroupDTO> findGroup(String searchString, Authentication authentication) throws UsernameNotFoundException {
        checkAuthentication(authentication, -1);
        List<Group> foundPerson = groupRepository.findByCommonNameContainsIgnoreCaseOrderByCommonNameAsc(searchString);
        return foundPerson.stream().map(
                authMapper::fromModel
        ).toList();
    }

    private void checkAuthentication(Authentication authentication, int errorCode) {
        String callerMethodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        assertion(
                ()->authentication != null && authentication.isAuthenticated(),
                NotAuthenticated.notAuthenticatedBuilder()
                        .errorCode(errorCode)
                        .errorDomain(callerMethodName)
                        .build()
        );
    }
}
