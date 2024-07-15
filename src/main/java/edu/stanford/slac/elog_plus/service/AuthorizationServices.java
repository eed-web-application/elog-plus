package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
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
}
