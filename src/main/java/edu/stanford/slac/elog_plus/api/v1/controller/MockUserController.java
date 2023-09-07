package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.auth.test_mock_auth.JWTHelper;
import edu.stanford.slac.elog_plus.ldap_repository.PersonRepository;
import edu.stanford.slac.elog_plus.model.Person;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@RestController()
@RequestMapping("/v1/mock")
@AllArgsConstructor
@Profile("test")
public class MockUserController {
    PersonRepository personRepository;

    @GetMapping(
            path = "/users-auth",
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<Map<String, String>> getMockUser() {
        Map<String, String> mockUserJWT = new HashMap<>();
        List<Person> persons = personRepository.findAll();
        for (Person p:
                persons) {
            mockUserJWT.put(
                    p.getGecos(),
                    JWTHelper.generateJwt(p.getMail())
            );
        }
        return ApiResultResponse.of(
                mockUserJWT
        );
    }
}
