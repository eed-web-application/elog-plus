package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthMapper;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.NotAuthenticated;
import edu.stanford.slac.elog_plus.exception.UserNotFound;
import edu.stanford.slac.elog_plus.ldap_repository.GroupRepository;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Group;
import edu.stanford.slac.elog_plus.model.Person;
import edu.stanford.slac.elog_plus.ldap_repository.PersonRepository;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Root;

@Service
@Log4j2
@AllArgsConstructor
public class AuthService {
    private final AuthMapper authMapper;
    private final AppProperties appProperties;
    private final PersonRepository personRepository;
    private final GroupRepository groupRepository;
    private final AuthorizationRepository authorizationRepository;

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

    public void updateRootUser() {
        log.info("Find current authorizations");
        //load actual root
        List<Authorization> currentRootUser = authorizationRepository.findByResourceIsAndAuthorizationTypeIs(
                "*",
                Root
        );

        // find root users to remove
        List<String> rootUserToRemove = currentRootUser.stream().map(
                Authorization::getOwner
        ).toList().stream().filter(
                userEmail-> !appProperties.getRootUserList().contains(userEmail)
        ).toList();
        for (String userEmailToRemove :
                rootUserToRemove) {
            log.info("Remove root authorizations: {}", userEmailToRemove);
            authorizationRepository.deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(
                    userEmailToRemove,
                    "*",
                    Root
            );
        }

        // ensure current root users
        log.info("Ensure root authorizations for: {}", appProperties.getRootUserList());
        for (String userEmail :
                appProperties.getRootUserList()) {
            // find root authorization for user email
            Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIs(
                    userEmail,
                    "*",
                    Root
            );
            if (rootAuth.isEmpty()) {
                log.info("Create root authorization for user '{}'", userEmail);
                authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(Root)
                                .owner(userEmail)
                                .resource("*")
                                .creationBy("elog-plus")
                                .build()
                );
            } else {
                log.info("Root authorization for '{}' already exists", userEmail);
            }
        }
    }
}
