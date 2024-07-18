package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.*;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.LocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.LocalGroupQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.auth.jwt.SLACAuthenticationJWTToken;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthorizationMapper;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Log4j2
@AllArgsConstructor
public class AuthorizationServices {
    AuthService authService;
    PeopleGroupService peopleGroupService;
    AuthorizationMapper authorizationMapper;

    /**
     * Find users based on the query parameter
     *
     * @param personQueryParameterDTO the query parameter
     * @return the list of users found
     */
    public List<UserDetailsDTO> findUsers(PersonQueryParameterDTO personQueryParameterDTO, Boolean includeAuthorizations) {
        // found users
        var foundUsers = peopleGroupService.findPersons(personQueryParameterDTO);
        //convert to UserDetailsDTO
        return foundUsers.stream().map(
                u -> authorizationMapper.fromPersonDTO
                        (
                                u,
                                includeAuthorizations?authorizationMapper.fromAuthorizationDTO(
                                        authService.getAllAuthenticationForOwner(
                                                u.mail(),
                                                AuthorizationOwnerTypeDTO.User,
                                                Optional.empty()
                                        )
                                ):Collections.emptyList()
                        )

        ).toList();
    }

    /**
     * Find a group
     *
     * @param localGroupId          the id of the group to find
     * @param includeAuthorizations if true include the authorizations
     * @return the group details
     */
    public GroupDetailsDTO findGroup(String localGroupId, Boolean includeMembers, Boolean includeAuthorizations) {
        // find the group
        LocalGroupDTO groupFound = authService.findLocalGroupById(localGroupId);
        return GroupDetailsDTO.builder()
                .id(groupFound.id())
                .name(groupFound.name())
                .description(groupFound.description())
                .members
                        (
                                includeMembers ?
                                        groupFound.members().stream().map(
                                                m -> authorizationMapper.fromPersonDTO(
                                                        m,
                                                        // user details on group doesn't has authorizations
                                                        Collections.emptyList()
                                                )
                                        ).toList() :
                                        Collections.emptyList()
                        )
                .authorizations
                        (
                                includeAuthorizations ?
                                        authorizationMapper.fromAuthorizationDTO
                                                (
                                                        authService.getAllAuthenticationForOwner
                                                                (
                                                                        groupFound.name(),
                                                                        AuthorizationOwnerTypeDTO.Group,
                                                                        Optional.empty()
                                                                )
                                                ) :
                                        Collections.emptyList()
                        )
                .build();

    }

    /**
     * Find groups based on the query parameter
     *
     * @param localGroupQueryParameterDTO the query parameter
     * @param includeAuthorizations       if true include the authorizations
     * @return the list of groups found
     */
    public List<GroupDetailsDTO> findGroups(
            LocalGroupQueryParameterDTO localGroupQueryParameterDTO,
            Boolean includeMembers,
            Boolean includeAuthorizations
    ) {
        var foundGroups = authService.findLocalGroup(localGroupQueryParameterDTO);
        return foundGroups.stream().map(
                g -> GroupDetailsDTO.builder()
                        .id(g.id())
                        .name(g.name())
                        .description(g.description())
                        .members(
                                includeMembers ?
                                        g.members().stream().map(
                                                m -> authorizationMapper.fromPersonDTO(
                                                        m,
                                                        // user details on group doesn't has authorizations
                                                        Collections.emptyList()
                                                )
                                        ).toList() :
                                        Collections.emptyList()
                        )
                        .authorizations(
                                includeAuthorizations ?
                                        authorizationMapper.fromAuthorizationDTO
                                                (
                                                        authService.getAllAuthenticationForOwner
                                                                (
                                                                        g.name(),
                                                                        AuthorizationOwnerTypeDTO.Group,
                                                                        Optional.empty()
                                                                )
                                                ) :
                                        Collections.emptyList()
                        )
                        .build()
        ).toList();
    }

    /**
     * Find all applications
     *
     * @param authenticationTokenQueryParameterDTO the query parameter
     * @param includeMembers                       if true include the members
     * @param includeAuthorizations                if true include the authorizations
     * @return the list of applications found
     */
    public List<ApplicationDetailsDTO> findAllApplications(
            AuthenticationTokenQueryParameterDTO authenticationTokenQueryParameterDTO,
            Boolean includeMembers,
            Boolean includeAuthorizations
    ) {
        var authTokenFound = authService.findAllAuthenticationToken(authenticationTokenQueryParameterDTO);
        return authTokenFound.stream().map(
                a -> ApplicationDetailsDTO.builder()
                        .id(a.id())
                        .name(a.name())
                        .email(a.email())
                        .token(a.token())
                        .expiration(a.expiration())
                        .applicationManaged(a.applicationManaged())
                        .authorizations(
                                includeAuthorizations ?
                                        authorizationMapper.fromAuthorizationDTO
                                                (
                                                        authService.getAllAuthenticationForOwner
                                                                (
                                                                        a.name(),
                                                                        AuthorizationOwnerTypeDTO.Token,
                                                                        Optional.empty()
                                                                )
                                                ) :
                                        Collections.emptyList()
                        )
                        .build()
        ).toList();
    }

    /**
     * Create a new authorization
     *
     * @param newAuthorizationDTO the new authorization to create
     */
    public void createNew(NewAuthorizationDTO newAuthorizationDTO) {
        // check the resource type
        String resource = getResource(newAuthorizationDTO);
        authService.addNewAuthorization(
                edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO
                        .builder()
                        .owner(newAuthorizationDTO.ownerId())
                        .ownerType(newAuthorizationDTO.ownerType())
                        .authorizationType(newAuthorizationDTO.authorizationType())
                        .resource(resource)
                        .build()
        );
        log.info(
                "Created new authorization on {}/{} for {}/{} by {}",
                newAuthorizationDTO.resourceType(), newAuthorizationDTO.resourceId(),
                newAuthorizationDTO.ownerType(), newAuthorizationDTO.ownerId(),
                getCurrentUsername()
        );
    }

    /**
     * Delete an authorization
     *
     * @param authorizationId the id of the authorization to delete
     */
    public void deleteAuthorization(String authorizationId) {
        var authorizationFound = authService.findAuthorizationById(authorizationId);
        log.info("Deleting authorization {} by {}", authorizationFound, getCurrentUsername());
        authService.deleteAuthorizationById(authorizationId);
    }

    /**
     * Update an authorization
     *
     * @param authorizationId        the id of the authorization to update
     * @param updateAuthorizationDTO the update information
     */
    @Transactional
    public void updateAuthorization(String authorizationId, UpdateAuthorizationDTO updateAuthorizationDTO) {
        //TODO: update the authorization without delete it
        var authorizationFound = authService.findAuthorizationById(authorizationId);
        authService.deleteAuthorizationById(authorizationId);
        authService.addNewAuthorization(
                edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO
                        .builder()
                        .owner(authorizationFound.owner())
                        .ownerType(authorizationFound.ownerType())
                        .resource(authorizationFound.resource())
                        .authorizationType(updateAuthorizationDTO.authorizationType())
                        .build()
        );
    }

    /**
     * Create a new application
     *
     * @param newApplicationDTO the new application to create
     * @return the id of the new application
     */
    public String createNewApplication(NewApplicationDTO newApplicationDTO) {
        var createdAuthToken =  authService.addNewApplicationAuthenticationToken(
                NewAuthenticationTokenDTO
                        .builder()
                        .name(newApplicationDTO.name())
                        .expiration(newApplicationDTO.expiration())
                        .build(),
                false);
        return createdAuthToken.id();
    }

    /**
     * Delete an application
     *
     * @param applicationId the id of the application to delete
     */
    public void deleteApplication(String applicationId) {
        authService.deleteToken(applicationId);
    }

    public ApplicationDetailsDTO getApplicationById(String applicationId) {
        var authTokenFound = authService.getAuthenticationTokenById(applicationId).orElseThrow(
                ()->ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage("Application not found")
                        .errorDomain("AuthorizationServices::getApplicationById")
                        .build()
        );
        return ApplicationDetailsDTO.builder()
                .id(authTokenFound.id())
                .name(authTokenFound.name())
                .email(authTokenFound.email())
                .token(authTokenFound.token())
                .expiration(authTokenFound.expiration())
                .applicationManaged(authTokenFound.applicationManaged())
                .build();
    }

    /**
     * Get the current username
     *
     * @return the current username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof SLACAuthenticationJWTToken) {
                return ((SLACAuthenticationJWTToken) principal).getPrincipal().toString();
            } else {
                return principal.toString();
            }
        }
        return null;
    }

    /**
     * Create the resource analyzing the resourceType and resource Id
     *
     * @param newAuthorizationDTO
     * @return
     */
    public String getResource(NewAuthorizationDTO newAuthorizationDTO) {
        switch (newAuthorizationDTO.resourceType()) {
            case Logbook:
                return "/logbook/%s".formatted(newAuthorizationDTO.resourceId());
            case All:
                return "*";
            default:
                throw ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage("Resource type not found")
                        .errorDomain("AuthorizationServices::getResource")
                        .build();
        }
    }

}
