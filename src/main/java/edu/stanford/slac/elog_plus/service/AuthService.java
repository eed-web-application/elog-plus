package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogbookMapper;
import edu.stanford.slac.elog_plus.auth.JWTHelper;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.*;
import edu.stanford.slac.elog_plus.ldap_repository.GroupRepository;
import edu.stanford.slac.elog_plus.model.*;
import edu.stanford.slac.elog_plus.ldap_repository.PersonRepository;
import edu.stanford.slac.elog_plus.repository.AuthenticationTokenRepository;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import edu.stanford.slac.elog_plus.utility.StringUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.*;
import static edu.stanford.slac.elog_plus.utility.StringUtilities.tokenNameNormalization;

@Service
@Log4j2
@AllArgsConstructor
public class AuthService {
    private final JWTHelper jwtHelper;
    private final AuthMapper authMapper;
    private final AppProperties appProperties;
    private final PersonRepository personRepository;
    private final GroupRepository groupRepository;
    private final AuthenticationTokenRepository authenticationTokenRepository;
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

    /**
     * Find the group by filtering on name
     *
     * @param searchString search string for the group name
     * @return the list of found groups
     */
    public List<GroupDTO> findGroup(String searchString) throws UsernameNotFoundException {
        List<Group> foundPerson = groupRepository.findByCommonNameContainsIgnoreCaseOrderByCommonNameAsc(searchString);
        return foundPerson.stream().map(
                authMapper::fromModel
        ).toList();
    }

    /**
     * find all group for the user
     *
     * @param userId is the user id
     * @return the list of the groups where the user belong
     */
    private List<GroupDTO> findGroupByUserId(String userId) {
        List<Group> findGroups = groupRepository.findByMemberUidContainingIgnoreCase(userId);
        return findGroups.stream().map(
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
    @Cacheable(value = "user-root-authorization", key = "{#authentication.credentials}", unless = "#authentication == null")
    public boolean checkForRoot(Authentication authentication) {
        if (!checkAuthentication(authentication)) return false;
        // only root user can create logbook
        List<AuthorizationDTO> foundAuth = getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                authentication.getCredentials().toString(),
                Admin,
                "*",
                Optional.empty()
        );
        return foundAuth != null && !foundAuth.isEmpty();
    }

    /**
     * Check the authorizations level on a resource, the authorizations found
     * will be all those authorizations that will have the value of authorizations type greater
     * or equal to the one give as argument. This return true also if the current authentication
     * is an admin. The api try to search if the user is authorized using user, groups or application checks
     *
     * @param authorization  the minimum value of authorizations to check
     * @param authentication the current authentication
     * @param resourcePrefix the target resource
     */
    @Cacheable(value = "user-authorization", key = "{#authentication.credentials, #authorization, #resourcePrefix}", unless = "#authentication == null")
    public boolean checkAuthorizationForOwnerAuthTypeAndResourcePrefix(Authentication authentication, Authorization.Type authorization, String resourcePrefix) {
        if (!checkAuthentication(authentication)) return false;
        if (checkForRoot(authentication)) return true;
        List<AuthorizationDTO> foundLogbookAuth = getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                authentication.getCredentials().toString(),
                authorization,
                resourcePrefix,
                Optional.empty()
        );
        return !foundLogbookAuth.isEmpty();
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

    @Cacheable(value = "user-authorization", key = "{#owner, #authorizationType, #resourcePrefix}")
    public List<AuthorizationDTO> getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
            String owner,
            Authorization.Type authorizationType,
            String resourcePrefix) {
        return getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                owner,
                authorizationType,
                resourcePrefix,
                Optional.empty()
        );
    }

    /**
     * Return all the authorizations for an owner that match with the prefix
     * and the authorizations type, if the owner is of type 'User' will be checked for all the
     * entries all along with those that belongs to all the user groups.
     *
     * @param owner                       si the owner target of the result authorizations
     * @param authorizationType           filter on the @Authorization.Type
     * @param resourcePrefix              is the prefix of the authorized resource
     * @param allHigherAuthOnSameResource return only the higher authorization for each resource
     * @return the list of found resource
     */
    @Cacheable(value = "user-authorization", key = "{#owner, #authorizationType, #resourcePrefix, #allHigherAuthOnSameResource}")
    public List<AuthorizationDTO> getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
            String owner,
            Authorization.Type authorizationType,
            String resourcePrefix,
            Optional<Boolean> allHigherAuthOnSameResource
    ) {
        boolean isAppToken = isAppTokenEmail(owner);
        // get user authorizations
        List<AuthorizationDTO> allAuth = new ArrayList<>(
                wrapCatch(
                        () -> authorizationRepository.findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(
                                owner,
                                isAppToken ? Authorization.OType.Application : Authorization.OType.User,
                                authorizationType.getValue(),
                                resourcePrefix
                        ),
                        -1,
                        "AuthService::getAllAuthorization"
                ).stream().map(
                        authMapper::fromModel
                ).toList()
        );

        // get user authorizations inherited by group
        if (!isAppToken) {
            // in case we have a user check also the groups that belongs to the user
            List<GroupDTO> userGroups = findGroupByUserId(owner);

            // load all groups authorizations
            allAuth.addAll(
                    userGroups
                            .stream()
                            .map(
                                    g -> authorizationRepository.findByOwnerAndOwnerTypeAndAuthorizationTypeIsGreaterThanEqualAndResourceStartingWith(
                                            g.commonName(),
                                            Authorization.OType.Group,
                                            authorizationType.getValue(),
                                            resourcePrefix

                                    )
                            )
                            .flatMap(List::stream)
                            .map(
                                    authMapper::fromModel
                            )
                            .toList()
            );
        }
        if (allHigherAuthOnSameResource.isPresent() && allHigherAuthOnSameResource.get()) {
            allAuth = allAuth.stream()
                    .collect(
                            Collectors.toMap(
                                    AuthorizationDTO::resource,
                                    auth -> auth,
                                    (existing, replacement) ->
                                            Authorization.Type.valueOf(existing.authorizationType()).getValue() >= Authorization.Type.valueOf(replacement.authorizationType()).getValue() ? existing : replacement
                            ))
                    .values().stream().toList();
        }
        return allAuth;
    }

    /**
     * Update all configured root user
     */
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
                                .ownerType(Authorization.OType.User)
                                .resource("*")
                                .creationBy("elog-plus")
                                .build()
                );
            } else {
                log.info("Root authorizations for '{}' already exists", userEmail);
            }
        }
    }

    /**
     * Add a new authentication token
     *
     * @param newAuthenticationTokenDTO is the new token information
     */
    public AuthenticationTokenDTO addNewAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO) {
        // check if a token with the same name already exists
        assertion(
                AuthenticationTokenMalformed.malformedAuthToken()
                        .errorCode(-1)
                        .errorDomain("AuthService::addNewAuthenticationToken")
                        .build(),
                // name well-formed
                () -> newAuthenticationTokenDTO.name() != null && !newAuthenticationTokenDTO.name().isEmpty(),
                // expiration well-formed
                () -> newAuthenticationTokenDTO.expiration() != null
        );

        assertion(
                () -> wrapCatch(
                        () -> !authenticationTokenRepository.existsByName(newAuthenticationTokenDTO.name()),
                        -2,
                        "AuthService::addNewAuthenticationToken"
                ),
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("A token with the same name already exists")
                        .errorDomain("AuthService::addNewAuthenticationToken")
                        .build()
        );
        // convert to model and normalize the name
        AuthenticationToken authTok = authMapper.toModelToken(
                newAuthenticationTokenDTO.toBuilder()
                        .name(
                                tokenNameNormalization(
                                        newAuthenticationTokenDTO.name()
                                )
                        )
                        .build()
        );
        return authMapper.toTokenDTO(
                wrapCatch(
                        () -> authenticationTokenRepository.save(
                                authTok.toBuilder()
                                        .token(
                                                jwtHelper.generateAuthenticationToken(
                                                        authTok
                                                )
                                        )
                                        .build()
                        ),
                        -4,
                        "AuthService::addNewAuthenticationToken"
                )
        );
    }

    /**
     * Return an application token by name
     *
     * @param name the name of the token to return
     * @return the found authentication token
     */
    public Optional<AuthenticationTokenDTO> getAuthenticationTokenByName(String name) {
        return wrapCatch(
                () -> authenticationTokenRepository.findByName(name)
                        .map(
                                authMapper::toTokenDTO
                        ),
                -1,
                "AuthService::getAuthenticationTokenByName"
        );
    }

    /**
     * return al the global authentication tokens
     * @return the list of all authentication tokens
     */
    public List<AuthenticationTokenDTO> getAllAuthenticationToken() {
        return wrapCatch(
                () -> authenticationTokenRepository.findAll()
                        .stream()
                        .map(
                                authMapper::toTokenDTO
                        ).toList(),
                -1,
                "AuthService::getAuthenticationTokenByName"
        );
    }

    /**
     * Delete a token by name along with all his authorization records
     * @param id the token id
     */
    @Transactional
    public void deleteToken(String id){
        AuthenticationTokenDTO tokenToDelete =  getAuthenticationTokenById(id)
                .orElseThrow(
                        ()->AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                .errorCode(-1)
                                .errorDomain("AuthService::deleteToken")
                                .build()
                );

        //delete authorizations
        wrapCatch(
                ()-> {authorizationRepository.deleteAllByOwnerIs(tokenToDelete.email());return null;},
                -2,
                "AuthService::deleteToken"
        );
        // delete token
        wrapCatch(
                ()-> {authenticationTokenRepository.deleteById(tokenToDelete.id());return null;},
                -3,
                "AuthService::deleteToken"
        );
    }

    /**
     * Return an application token by name
     *
     * @param id the unique id of the token to return
     * @return the found authentication token
     */
    public Optional<AuthenticationTokenDTO> getAuthenticationTokenById(String id) {
        return wrapCatch(
                () -> authenticationTokenRepository.findById(id)
                        .map(
                                authMapper::toTokenDTO
                        ),
                -1,
                "AuthService::getAuthenticationTokenByName"
        );
    }

    /**
     * Check if the email ends with the ELOG application fake domain without the logname
     *
     * @param email is the email to check
     * @return true is the email belong to autogenerated application token email
     */
    private boolean isAppTokenEmail(String email) {
        if (email == null) return false;
        return email.endsWith(appProperties.getApplicationTokenDomain());
    }
}
