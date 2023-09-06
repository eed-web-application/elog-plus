package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.exception.NotAuthenticated;
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
    private TestHelperService testHelperService;

    @Test
    public void getMe() {
        ApiResultResponse<PersonDTO> meResult = assertDoesNotThrow(
                () -> testHelperService.getMe(
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
        NotAuthenticated userNotFoundException = assertThrows(
                NotAuthenticated.class,
                () -> testHelperService.getMe(
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
                () -> testHelperService.findUsers(
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
    public void findUsersFailUnauthorized() {
        NotAuthenticated userNotFoundException = assertThrows(
                NotAuthenticated.class,
                () -> testHelperService.findUsers(
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
                () -> testHelperService.findGroups(
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
        NotAuthenticated userNotFoundException = assertThrows(
                NotAuthenticated.class,
                () -> testHelperService.findGroups(
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
