package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.ResourceTypeDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.DetailsAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetailsDTO;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = TagMapper.class,
        componentModel = "spring"
)
public abstract class AuthorizationMapper {
    @Autowired
    AuthService authService;
    @Autowired
    LogbookRepository logbookRepository;
    /**
     * Convert a PersonDTO to a UserDetailsDTO
     *
     * @param personDTO                the personDTO to convert
     * @param userAuthorizationDTOList the list of authorizations for the user
     * @return the converted UserDetailsDTO
     */
    public UserDetailsDTO fromPersonDTO(PersonDTO personDTO, List<DetailsAuthorizationDTO> userAuthorizationDTOList) {
        if (personDTO == null) {
            return null;
        }
        return UserDetailsDTO.builder()
                .id(personDTO.mail())
                .name(personDTO.commonName())
                .surname(personDTO.surname())
                .email(personDTO.mail())
                .isRoot(authService.checkForRoot(personDTO.mail()))
                .canManageGroup
                        (
                                authService.canManageGroup(personDTO.mail())
                        )
                .authorizations(userAuthorizationDTOList==null?Collections.emptyList():userAuthorizationDTOList)
                .build();
    }

    /**
     * Convert a list of AuthorizationDTO to a list of UserAuthorizationDTO
     *
     * @param allAuthenticationForOwner the list of AuthorizationDTO to convert
     * @return the converted list of UserAuthorizationDTO
     */
    public List<DetailsAuthorizationDTO> fromAuthorizationDTO(
            List<AuthorizationDTO> allAuthenticationForOwner
    ) {
        HashMap<String, String> resourceMapIdLabel = new HashMap<>();
        if (allAuthenticationForOwner == null) {
            return Collections.emptyList();
        }
        return allAuthenticationForOwner
                .stream()
                .filter(a -> a.resource() != null)
                .map(
                        a -> {
                            String label = null;
                            ResourceTypeDTO resourceType = getResourceType(a.resource());
                            String resourceId = getResourceId(a.resource());

                            if(resourceMapIdLabel.containsKey(a.resource())){
                                label = resourceMapIdLabel.get(a.resource());
                            } else {
                                // fetch resourceType and cache
                                switch (resourceType) {
                                    case Logbook: {
                                        var logbook = logbookRepository.findById(resourceId);
                                        if (logbook.isPresent()) {
                                            label = logbook.get().getName();
                                            resourceMapIdLabel.put(a.resource(), label);
                                        }
                                        break;
                                    }
                                    default:
                                        break;
                                }
                            }
                            return DetailsAuthorizationDTO.builder()
                                    .id(a.id())
                                    .resourceType(resourceType)
                                    .resourceId(resourceId)
                                    .permission(a.authorizationType())
                                    .resourceName(label)
                                    .build();
                        }
                )
                .toList();
    }

    /**
     * Get the resourceType type from the resourceType string
     *
     * @param resource the resourceType string
     * @return the resourceType id
     */
    private ResourceTypeDTO getResourceType(String resource) {
        // the resourceType are of type '/logbook/logbookId' we need to return ''logbook' as resourceType
        String[] resourceAndType = resource.split("/");
        if (resourceAndType.length == 1 && resourceAndType[0].compareToIgnoreCase("*") == 0) {
            return ResourceTypeDTO.All;
        } else if (resourceAndType.length > 1) {
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
                    .errorMessage("Invalid resourceType in authorization")
                    .errorDomain("AuthorizationMapper::getResourceType")
                    .build();
        }
    }

    /**
     * Get the resourceType id from the resourceType string
     *
     * @param resource the resourceType string
     * @return the resourceType id
     */
    private String getResourceId(String resource) {
        // the resourceType are of type '/logbook/logbookId' we  need to return the string value 'logbookId'
        String[] resourceAndType = resource.split("/");
        if (resourceAndType.length == 1 && resourceAndType[0].compareToIgnoreCase("*") == 0) {
            return null;
        } else if (resourceAndType.length > 1) {
            return resourceAndType[2];
        } else {
            throw ControllerLogicException.builder()
                    .errorCode(-2)
                    .errorMessage("Invalid resourceType in authorization")
                    .errorDomain("AuthorizationMapper::getResourceId")
                    .build();
        }
    }
}
