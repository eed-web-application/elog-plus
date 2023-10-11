package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthMapper;
import edu.stanford.slac.elog_plus.auth.JWTHelper;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.*;
import edu.stanford.slac.elog_plus.ldap_repository.GroupRepository;
import edu.stanford.slac.elog_plus.ldap_repository.PersonRepository;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Group;
import edu.stanford.slac.elog_plus.model.Person;
import edu.stanford.slac.elog_plus.repository.AuthenticationTokenRepository;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationTypeDTO.Admin;
import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;
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
    public boolean checkAuthorizationForOwnerAuthTypeAndResourcePrefix(Authentication authentication, AuthorizationTypeDTO authorization, String resourcePrefix) {
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
            AuthorizationTypeDTO authorizationType,
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
            AuthorizationTypeDTO authorizationType,
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
                                authMapper.toModel(authorizationType).getValue(),
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
                                            authMapper.toModel(authorizationType).getValue(),
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

                                            authMapper.toModel(existing.authorizationType()).getValue() >= authMapper.toModel(replacement.authorizationType()).getValue() ? existing : replacement
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
        List<Authorization> currentRootUser = wrapCatch(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "*",
                        authMapper.toModel(Admin).getValue()
                ),
                -1,
                "AuthService::updateRootUser"
        );

        // find root users to remove
        List<String> rootUserToRemove = currentRootUser.stream().map(
                Authorization::getOwner
        ).toList().stream().filter(
                userEmail -> !wrapCatch(
                        () -> appProperties.getRootUserList().contains(userEmail),
                        -2,
                        "AuthService::updateRootUser"
                )
        ).toList();
        for (String userEmailToRemove :
                rootUserToRemove) {
            log.info("Remove root authorizations: {}", userEmailToRemove);
            wrapCatch(
                    () -> {
                        authorizationRepository.deleteByOwnerIsAndResourceIsAndAuthorizationTypeIs(
                                userEmailToRemove,
                                "*",
                                authMapper.toModel(Admin).getValue()
                        );
                        return null;
                    },
                    -2,
                    "AuthService::updateRootUser"
            );
        }

        // ensure current root users
        log.info("Ensure root authorizations for: {}", appProperties.getRootUserList());
        for (String userEmail :
                appProperties.getRootUserList()) {
            // find root authorizations for user email
            Optional<Authorization> rootAuth = wrapCatch(
                    () -> authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                            userEmail,
                            "*",
                            authMapper.toModel(Admin).getValue()
                    ),
                    -2,
                    "AuthService::updateRootUser"
            );
            if (rootAuth.isEmpty()) {
                log.info("Create root authorizations for user '{}'", userEmail);
                wrapCatch(
                        () -> authorizationRepository.save(
                                Authorization
                                        .builder()
                                        .authorizationType(authMapper.toModel(Admin).getValue())
                                        .owner(userEmail)
                                        .ownerType(Authorization.OType.User)
                                        .resource("*")
                                        .creationBy("elog-plus")
                                        .build()
                        ),
                        -2,
                        "AuthService::updateRootUser"
                );
            } else {
                log.info("Root authorizations for '{}' already exists", userEmail);
            }
        }
    }

    /**
     *
     */
    @Transactional
    public void updateAutoManagedRootToken() {
        log.info("Find current authentication token app managed");
        //load actual root
        if (appProperties.getRootAuthenticationTokenList().isEmpty()) {
            List<AuthenticationToken> foundAuthenticationTokens = wrapCatch(
                    () -> wrapCatch(
                                authenticationTokenRepository::findAllByApplicationManagedIsTrue,
                                -1,
                                "AuthService::updateAutoManagedRootToken"
                        ),
                    -2,
                    "AuthService::updateAutoManagedRootToken"
            );
            for (AuthenticationToken authToken:
                    foundAuthenticationTokens) {
                wrapCatch(
                        () -> {
                           authenticationTokenRepository.deleteById(
                                    authToken.getId()
                            );
                            return null;
                        },
                        -3,
                        "AuthService::updateAutoManagedRootToken"
                );
            }
            wrapCatch(
                    () -> {authenticationTokenRepository.deleteAllByApplicationManagedIsTrue(); return null;},
                    -4,
                    "AuthService::updateAutoManagedRootToken"
            );
        }
        List<AuthenticationToken> foundAuthenticationTokens = wrapCatch(
                authenticationTokenRepository::findAllByApplicationManagedIsTrue,
                -5,
                "AuthService::updateAutoManagedRootToken"
        );

        // check which we need to create
        for (
                NewAuthenticationTokenDTO newAuthenticationTokenDTO :
                appProperties.getRootAuthenticationTokenList()
        ) {
            var toCreate = foundAuthenticationTokens
                    .stream()
                    .filter(t -> t.getName().compareToIgnoreCase(newAuthenticationTokenDTO.name()) == 0)
                    .findAny().isEmpty();
            if (toCreate) {
                var newAuthTok = wrapCatch(
                        () -> authenticationTokenRepository.save(
                                getAuthenticationToken(
                                        newAuthenticationTokenDTO,
                                        true
                                )
                        ),
                        -6,
                        "AuthService::updateAutoManagedRootToken"
                );
                log.info("Created authentication token with name {}", newAuthTok.getName());
                wrapCatch(
                        () -> authorizationRepository.save(
                                Authorization
                                        .builder()
                                        .authorizationType(authMapper.toModel(Admin).getValue())
                                        .owner(newAuthTok.getEmail())
                                        .ownerType(Authorization.OType.Application)
                                        .resource("*")
                                        .creationBy("elog")
                                        .build()
                        ),
                        -7,
                        "AuthService::updateAutoManagedRootToken"
                );

                log.info("Created root authorization for token with name {}", newAuthTok.getName());
            }
        }

        // check which we need to remove
        for (
                AuthenticationToken foundAuthenticationToken :
                foundAuthenticationTokens
        ) {
            var toDelete = appProperties.getRootAuthenticationTokenList()
                    .stream()
                    .filter(t -> t.name().compareToIgnoreCase(foundAuthenticationToken.getName()) == 0)
                    .findAny().isEmpty();
            if (toDelete) {
                log.info("Delete authentication token for id {}", foundAuthenticationToken.getName());
                wrapCatch(
                        () -> {
                            deleteToken(
                                    foundAuthenticationToken.getId()
                            );
                            return null;
                        },
                        -8,
                        "AuthService::updateAutoManagedRootToken"
                );
            }
        }
    }

    /**
     * Add user identified by email as root
     *
     * @param email the user email
     */
    public void addRootAuthorization(String email, String creator) {
        boolean isAppToken = isAppTokenEmail(email);

        // check fi the user or app token exists
        if (isAppToken) {
            // give error in case of a logbook token(it cannot be root
            assertion(
                    ControllerLogicException.builder()
                            .errorCode(-1)
                            .errorMessage("Logbook token cannot became root")
                            .errorDomain("AuthService::addRootAuthorization")
                            .build(),
                    () -> !isAppLogbookTokenEmail(email)
            );
            // create root for global token
            var authenticationTokenFound = authenticationTokenRepository
                    .findByEmailIs(email)
                    .orElseThrow(
                            () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                    .errorCode(-1)
                                    .errorDomain("AuthService::addRootAuthorization")
                                    .build()
                    );
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("Authentication Token managed byt the elog application cannot be managed by user")
                            .errorDomain("AuthService::addRootAuthorization")
                            .build(),
                    // should be not an application managed app token
                    () -> !authenticationTokenFound.getApplicationManaged()
            );
        } else {
            // find the user
            personRepository.findByMail(email)
                    .orElseThrow(
                            () -> PersonNotFound.personNotFoundBuilder()
                                    .errorCode(-1)
                                    .email(email)
                                    .errorDomain("AuthService::addRootAuthorization")
                                    .build()
                    );
        }

        // check if root authorization is already benn granted
        Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                email,
                "*",
                authMapper.toModel(Admin).getValue()
        );
        if (rootAuth.isPresent()) return;
        wrapCatch(
                () -> authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(authMapper.toModel(Admin).getValue())
                                .owner(email)
                                .ownerType(isAppToken ? Authorization.OType.Application : Authorization.OType.User)
                                .resource("*")
                                .creationBy(creator)
                                .build()
                ),
                -1,
                "AuthService::addRootAuthorization"
        );
    }

    /**
     * Remove user identified by email as root user
     *
     * @param email that identify the user
     */
    public void removeRootAuthorization(String email) {
        boolean isAppToken = isAppTokenEmail(email);
        if (isAppToken) {
            // check if the authentication token exists before remove
            var authenticationTokenFound = authenticationTokenRepository
                    .findByEmailIs(email)
                    .orElseThrow(
                            () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                    .errorCode(-1)
                                    .errorDomain("AuthService::removeRootAuthorization")
                                    .build()
                    );
            assertion(
                    ControllerLogicException
                            .builder()
                            .errorCode(-1)
                            .errorMessage("Authentication Token managed byt the elog application cannot be managed by user")
                            .errorDomain("AuthService::removeRootAuthorization")
                            .build(),
                    // should be not an application managed app token
                    () -> !authenticationTokenFound.getApplicationManaged()
            );
        }
        Optional<Authorization> rootAuth = authorizationRepository.findByOwnerIsAndResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                email,
                "*",
                authMapper.toModel(Admin).getValue()
        );
        if (rootAuth.isEmpty()) return;

        wrapCatch(
                () -> {
                    authorizationRepository.delete(rootAuth.get());
                    return null;
                },
                -1,
                "AuthService::addRootAuthorization"
        );
    }

    /**
     * Return all root authorization
     *
     * @return all the root authorization
     */
    public List<AuthorizationDTO> findAllRoot() {
        return wrapCatch(
                () -> authorizationRepository.findByResourceIs("*"),
                -1,
                "AuthService::findAllRoot"
        )
                .stream()
                .map(
                        authMapper::fromModel
                )
                .toList();
    }

    /**
     * Ensure token
     *
     * @param authenticationToken token to ensure
     */
    public String ensureAuthenticationToken(AuthenticationToken authenticationToken) {
        Optional<AuthenticationToken> token = wrapCatch(
                () -> authenticationTokenRepository.findByEmailIs(authenticationToken.getEmail()),
                -1,
                "AuthService:ensureAuthenticationToken"
        );
        if (token.isPresent()) return token.get().getId();

        authenticationToken.setName(
                tokenNameNormalization(
                        authenticationToken.getName()
                )
        );

        authenticationToken.setToken(
                jwtHelper.generateAuthenticationToken(
                        authenticationToken
                )
        );
        AuthenticationToken newToken = wrapCatch(
                () -> authenticationTokenRepository.save(
                        authenticationToken
                ),
                -2,
                "AuthService:ensureAuthenticationToken"
        );
        return newToken.getId();
    }
    public AuthenticationTokenDTO addNewAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO) {
        return addNewAuthenticationToken(newAuthenticationTokenDTO, false);
    }
    /**
     * Add a new authentication token
     *
     * @param newAuthenticationTokenDTO is the new token information
     */
    public AuthenticationTokenDTO addNewAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, boolean appManaged) {
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
        return authMapper.toTokenDTO(
                wrapCatch(
                        () -> authenticationTokenRepository.save(
                                getAuthenticationToken(
                                        newAuthenticationTokenDTO,
                                        appManaged
                                )
                        ),
                        -4,
                        "AuthService::addNewAuthenticationToken"
                )
        );
    }

    private AuthenticationToken getAuthenticationToken(NewAuthenticationTokenDTO newAuthenticationTokenDTO, boolean appManaged) {
        AuthenticationToken authTok = authMapper.toModelToken(
                newAuthenticationTokenDTO.toBuilder()
                        .name(
                                tokenNameNormalization(
                                        newAuthenticationTokenDTO.name()
                                )
                        )
                        .build()
        );
        return authTok.toBuilder()
                .applicationManaged(appManaged)
                .token(
                        jwtHelper.generateAuthenticationToken(
                                authTok
                        )
                )
                .build();
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
     *
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
     *
     * @param id the token id
     */
    @Transactional
    public void deleteToken(String id) {
        AuthenticationTokenDTO tokenToDelete = getAuthenticationTokenById(id)
                .orElseThrow(
                        () -> AuthenticationTokenNotFound.authTokenNotFoundBuilder()
                                .errorCode(-1)
                                .errorDomain("AuthService::deleteToken")
                                .build()
                );
        // delete token
        wrapCatch(
                () -> {
                    authenticationTokenRepository.deleteById(tokenToDelete.id());
                    return null;
                },
                -3,
                "AuthService::deleteToken"
        );
        //delete authorizations
        wrapCatch(
                () -> {
                    authorizationRepository.deleteAllByOwnerIs(tokenToDelete.email());
                    return null;
                },
                -2,
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
     * @param email
     * @return
     */
    public boolean existsAuthenticationTokenByEmail(String email) {
        return wrapCatch(
                () -> authenticationTokenRepository.existsByEmail(email),
                -1,
                "AuthService::existsAuthenticationTokenByEmail"
        );
    }

    /**
     * Return the authentication token by email
     * @param email the email of the authentication token to return
     * @return the authentication token found
     */
    public Optional<AuthenticationTokenDTO> getAuthenticationTokenByEmail(String email) {
        return wrapCatch(
                () -> authenticationTokenRepository.findByEmailIs(email),
                -1,
                "AuthService::existsAuthenticationTokenByEmail"
        ).map(
                authMapper::toTokenDTO
        );
    }

    /**
     * delete all the authorization where the email ends with the postfix
     *
     * @param emailPostfix the terminal string of the email
     */
    public void deleteAllAuthenticationTokenWithEmailEndWith(String emailPostfix) {
        wrapCatch(
                () -> {
                    authenticationTokenRepository.deleteAllByEmailEndsWith(emailPostfix);
                    return null;
                },
                -1,
                "AuthService::deleteAllAuthenticationTokenWithEmailEndWith"
        );
    }

    /**
     * Check if the email ends with the ELOG application fake domain without the logname
     *
     * @param email is the email to check
     * @return true is the email belong to autogenerated application token email
     */
    public boolean isAppTokenEmail(String email) {
        if (email == null) return false;
        return email.endsWith(appProperties.getApplicationTokenDomain());
    }

    public boolean isAppLogbookTokenEmail(String email) {
        final Pattern pattern = Pattern.compile(appProperties.getLogbookEmailRegex(), Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
