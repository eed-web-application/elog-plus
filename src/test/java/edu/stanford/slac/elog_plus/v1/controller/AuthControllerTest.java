package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PropertySource("classpath:application.yml")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @Test
    public void getMe() {
        ApiResultResponse<PersonDTO> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.getMe(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload().uid()).isEqualTo("user1");
    }

    @Test
    public void getMeFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.getMe(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty()
                )
        );

        assertThat(userNotFoundException).isNotNull();
        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void findUsersOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("name")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(2);
    }

    @Test
    public void findUsersByNameOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("Name1")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(1);
        assertThat(meResult.getPayload().get(0).gecos()).isEqualTo("Name1 Surname1");
    }

    @Test
    public void findUsersBySurnameOK() {
        ApiResultResponse<List<PersonDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("Surname1")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(1);
        assertThat(meResult.getPayload().get(0).gecos()).isEqualTo("Name1 Surname1");
    }

    @Test
    public void findUsersFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.findUsers(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.of("name")
                )
        );

        assertThat(userNotFoundException).isNotNull();
        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void findGroupsOK() {
        ApiResultResponse<List<GroupDTO>> meResult = assertDoesNotThrow(
                () -> testControllerHelperService.findGroups(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("group")
                )
        );

        assertThat(meResult).isNotNull();
        assertThat(meResult.getErrorCode()).isEqualTo(0);
        assertThat(meResult.getPayload()).hasSize(2);
    }

    @Test
    public void findGroupsFailUnauthorized() {
        NotAuthorized userNotFoundException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.findGroups(
                        mockMvc,
                        status().isUnauthorized(),
                        Optional.empty(),
                        Optional.of("group")
                )
        );

        assertThat(userNotFoundException).isNotNull();
        assertThat(userNotFoundException.getErrorCode()).isEqualTo(-1);
    }
}
