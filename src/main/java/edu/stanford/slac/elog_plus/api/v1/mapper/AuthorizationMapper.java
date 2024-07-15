package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.api.v1.dto.ResourceTypeDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = TagMapper.class,
        componentModel = "spring"
)
public abstract class AuthorizationMapper {

    /**
     * Convert a PersonDTO to a UserDetailsDTO
     * @param personDTO the personDTO to convert
     * @param userAuthorizationDTOList the list of authorizations for the user
     * @return the converted UserDetailsDTO
     */
    public UserDetailsDTO fromPersonDTO(PersonDTO personDTO, List<UserAuthorizationDTO> userAuthorizationDTOList) {
        if (personDTO == null) {
            return null;
        }

        return UserDetailsDTO.builder()
                .name(personDTO.commonName())
                .surname(personDTO.surname())
                .email(personDTO.mail())
                .authorization(
                        userAuthorizationDTOList
                )
                .build();
    }

    /**
     * Convert a list of AuthorizationDTO to a list of UserAuthorizationDTO
     * @param allAuthenticationForOwner the list of AuthorizationDTO to convert
     * @return the converted list of UserAuthorizationDTO
     */
    public List<UserAuthorizationDTO> fromAuthorizationDTO(
            List<AuthorizationDTO> allAuthenticationForOwner) {
        if (allAuthenticationForOwner == null) {
            return null;
        }
        return allAuthenticationForOwner
                .stream()
                .filter(a -> a.resource() != null)
                .map(
                        a -> UserAuthorizationDTO.builder()
                                .id(a.id())
                                .resourceType(getResourceType(a.resource()))
                                .resourceId(getResourceId(a.resource()))
                                .authorizationType(a.authorizationType())
                                .build()
                )
                .toList();
    }

    /**
     * Get the resource type from the resource string
     * @param resource the resource string
     * @return the resource id
     */
    private ResourceTypeDTO getResourceType(String resource) {
        // the resource are of type '/logbook/logbookId' we we need to return ''logbook' as resource
        String[] resourceAndType = resource.split("/");
        if(resourceAndType.length == 1 && resourceAndType[0].compareToIgnoreCase("*") == 0){
            return ResourceTypeDTO.All;
        } else if(resourceAndType.length > 1) {
            switch (resourceAndType[1]) {
                case "logbook":
                    return ResourceTypeDTO.Logbook;
                default:
                    throw ControllerLogicException.builder()
                            .errorCode(-1)
                            .errorMessage("Resource type not found")
                            .errorDomain("AuthorizationMapper::getResourceType")
                            .build();
            }
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-2)
                    .errorMessage("Invalid resource in authorization")
                    .errorDomain("AuthorizationMapper::getResourceType")
                    .build();
        }
    }

    /**
     * Get the resource id from the resource string
     * @param resource the resource string
     * @return the resource id
     */
    private String getResourceId(String resource) {
        // the resource are of type '/logbook/logbookId' we  need to return the string value 'logbookId'
        String[] resourceAndType = resource.split("/");
        if(resourceAndType.length == 1 && resourceAndType[0].compareToIgnoreCase("*") == 0){
            return null;
        } else if(resourceAndType.length > 1) {
            return resourceAndType[2];
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-2)
                    .errorMessage("Invalid resource in authorization")
                    .errorDomain("AuthorizationMapper::getResourceId")
                    .build();
        }
    }
}
