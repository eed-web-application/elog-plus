package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthMapper;
import edu.stanford.slac.elog_plus.config.AppProperties;
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
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.*;

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
        return personRepository.findByMail(
                authentication.getCredentials().toString()
        ).map(
                authMapper::fromModel
        ).orElseThrow(
                () -> UserNotFound.userNotFound()
                        .errorCode(-2)
                        .errorDomain("AuthService::findPerson")
                        .build()
        );
    }

    public List<PersonDTO> findPersons(String searchString) throws UsernameNotFoundException {
        List<Person> foundPerson = personRepository.findByGecosContainsIgnoreCaseOrderByCommonNameAsc(
                searchString
        );
        return foundPerson.stream().map(
                authMapper::fromModel
        ).toList();
    }

    public List<GroupDTO> findGroup(String searchString) throws UsernameNotFoundException {
        List<Group> foundPerson = groupRepository.findByCommonNameContainsIgnoreCaseOrderByCommonNameAsc(searchString);
        return foundPerson.stream().map(
                authMapper::fromModel
        ).toList();
    }

    /**
     * Check if the current authentication is authenticated
     *
     * @param authentication the current authentication
     */
    public boolean checkAuthentication(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    /**
     * Check if the current authentication is a root user
     *
     * @param authentication is the current authentication
     */
    public boolean checkForRoot(Authentication authentication) {
        if(!checkAuthentication(authentication)) return false;
        // only root user can create logbook
        List<AuthorizationDTO> foundAuth = getAllAuthorizationForOwnerAuthTypeAndResourcePrefix(
                authentication.getCredentials().toString(),
                Admin,
                "^\\*"
        );
        return foundAuth != null && !foundAuth.isEmpty();
    }

    /**
     * Check the authorizations level on a resource, the authorizations found
     * will be all those authorizations that will have the value of authorizations type greater
     * or equal to the one give as argument. This return true also if the current authentication
     * is an admin
     *
     * @param authorization  the minimum value of authorizations to check
     * @param authentication the current authentication
     * @param resource       the target resource
     */
    public boolean checkAuthorizationOForOwnerAuthTypeAndResourcePrefix(Authentication authentication, Authorization.Type authorization, String resource) {
        if(!checkAuthentication(authentication)) return false;
        List<AuthorizationDTO> foundLogbookAuth = getAllAuthorizationForOwnerAuthTypeAndResourcePrefix(
                authentication.getCredentials().toString(),
                authorization,
                resource
        );
        return !foundLogbookAuth.isEmpty();
    }


    /**
     * Create new authorizations
     *
     * @param authorization the new authorizations
     * @return updated authorizations
     */
    public AuthorizationDTO saveAuthorization(AuthorizationDTO authorization) {
        var savedAuth = wrapCatch(
                () -> authorizationRepository.save(
                        authMapper.toModel(authorization)
                ),
                -1,
                "AuthService::insertNewAuthorization"
        );
        return authMapper.fromModel(savedAuth);
    }

    /**
     * Delete an authorizations for a resource with a specific prefix
     *
     * @param resourcePrefix the prefix of the resource
     */
    public void deleteAuthorizationForResourcePrefix(String resourcePrefix) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByResourceStartingWith(
                            resourcePrefix
                    );
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationResourcePrefix"
        );
    }

    /**
     * Delete an authorizations for a resource with a specific path
     *
     * @param resource the path of the resource
     */
    public void deleteAuthorizationForResource(String resource) {
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByResourceIs(
                            resource
                    );
                    return null;
                },
                -1,
                "AuthService::deleteAuthorizationResourcePrefix"
        );
    }

    /**
     * Return all the authorizations for an owner that match with the prefix
     * and the authorizations type
     *
     * @param owner             si the owner target of the result authorizations
     * @param authorizationType filter on the @Authorization.Type
     * @param resourcePrefix    is the prefix of the authorized resource
     * @return the list of found resource
     */
    public List<AuthorizationDTO> getAllAuthorizationForOwnerAuthTypeAndResourcePrefix(
            String owner,
            Authorization.Type authorizationType,
            String resourcePrefix
    ) {
        return wrapCatch(
                () -> authorizationRepository.findByOwnerAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(
                        owner,
                        authorizationType.getValue(),
                        "^%s".formatted(resourcePrefix)
                ),
                -1,
                "AuthService::getAllAuthorization"
        ).stream().map(
                authMapper::fromModel
        ).toList();
    }

    public void updateRootUser() {
        log.info("Find current authorizations");
        //load actual root
        List<Authorization> currentRootUser = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Admin.getValue()
        );

        // find root users to remove
        List<String> rootUserToRemove = currentRootUser.stream().map(
                Authorization::getOwner
        ).toList().stream().filter(
                userEmail -> !appProperties.getRootUserList().contains(userEmail)
        ).toList();
        for (String userEmailToRemove :
                rootUserToRemove) {
            log.info("Remove root authorizations: {}", userEmailToRemove);
            authorizationRepository.deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(
                    userEmailToRemove,
                    "*",
                    Admin.getValue()
            );
        }

        // ensure current root users
        log.info("Ensure root authorizations for: {}", appProperties.getRootUserList());
        for (String userEmail :
                appProperties.getRootUserList()) {
            // find root authorizations for user email
            Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                    userEmail,
                    "*",
                    Admin.getValue()
            );
            if (rootAuth.isEmpty()) {
                log.info("Create root authorizations for user '{}'", userEmail);
                authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(Admin.getValue())
                                .owner(userEmail)
                                .resource("*")
                                .creationBy("elog-plus")
                                .build()
                );
            } else {
                log.info("Root authorizations for '{}' already exists", userEmail);
            }
        }
    }
}
