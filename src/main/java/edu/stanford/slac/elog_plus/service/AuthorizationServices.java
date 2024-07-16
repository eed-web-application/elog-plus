package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ResourceTypeDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetailsDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AuthorizationMapper;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

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
     * @param personQueryParameterDTO the query parameter
     * @return the list of users found
     */
    public List<UserDetailsDTO> findUsers(PersonQueryParameterDTO personQueryParameterDTO) {
        // found users
        var foundUsers = peopleGroupService.findPersons(personQueryParameterDTO);
        //convert to UserDetailsDTO
        return foundUsers.stream().map(
                u-> authorizationMapper.fromPersonDTO
                (
                        u,
                        authorizationMapper.fromAuthorizationDTO(
                            authService.getAllAuthenticationForOwner(
                                    u.mail(),
                                    AuthorizationOwnerTypeDTO.User,
                                    Optional.empty()
                            )
                        )
                )

        ).toList();
    }

    /**
     * Create a new authorization
     * @param newAuthorizationDTO the new authorization to create
     */
    public void createNew(NewAuthorizationDTO newAuthorizationDTO) {
        // check the resource type
        String resource = getResource(newAuthorizationDTO);
        if(resource.equals("*") && (newAuthorizationDTO.authorizationType() != AuthorizationTypeDTO.Admin)) {
            throw ControllerLogicException
                    .builder()
                    .errorCode(-1)
                    .errorMessage("The resource '*' can only be granted to Admin")
                    .errorDomain("AuthorizationServices::createNew")
                    .build();
        }
        authService.addNewAuthorization(
                edu.stanford.slac.ad.eed.baselib.api.v1.dto.NewAuthorizationDTO
                        .builder()
                        .owner(newAuthorizationDTO.ownerId())
                        .ownerType(newAuthorizationDTO.ownerTypeDTO())
                        .resource(resource)
                        .build()
        );
    }

    /**
     * Delete an authorization
     * @param authorizationId the id of the authorization to delete
     */
    public void deleteAuthorization(String authorizationId) {
        authService.deleteAuthorizationById(authorizationId);
    }

    /**
     * Create the resource analyzing the resourceType and resource Id
     * @param newAuthorizationDTO
     * @return
     */
    private String getResource(NewAuthorizationDTO newAuthorizationDTO) {
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
