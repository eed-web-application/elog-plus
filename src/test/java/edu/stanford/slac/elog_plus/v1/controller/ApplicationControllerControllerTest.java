package edu.stanford.slac.elog_plus.v1.controller;


import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.AuthenticationTokenNotFound;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewApplicationDTO;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ApplicationControllerControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TestControllerHelperService testControllerHelperService;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Entry.class);
        //reset authorizations
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        mongoTemplate.remove(new Query(), LocalGroup.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void createApp() {
        var resourceAppId = assertDoesNotThrow(
                () -> testControllerHelperService.applicationControllerCreateNewApplication(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewApplicationDTO.builder()
                                .name("app1")
                                .expiration(LocalDate.of(2100, 1, 1))
                                .build()
                )
        );
        assertThat(resourceAppId).isNotNull();
    }


    @Test
    public void deleteApp() {
        var resourceAppId = assertDoesNotThrow(
                () -> testControllerHelperService.applicationControllerCreateNewApplication(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewApplicationDTO.builder()
                                .name("app1")
                                .expiration(LocalDate.of(2100, 1, 1))
                                .build()
                )
        );
        assertThat(resourceAppId).isNotNull();

        //try to delete the app
        var deleteAppResult = assertDoesNotThrow(
                () -> testControllerHelperService.applicationControllerDeleteApplication(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        resourceAppId.getPayload()
                )
        );
        assertThat(deleteAppResult).isNotNull();
        assertThat(deleteAppResult.getPayload()).isTrue();

        // try to fetch the app again will cause exception
        AuthenticationTokenNotFound notFoundException = assertThrows(
                AuthenticationTokenNotFound.class,
                () -> testControllerHelperService.applicationControllerDeleteApplication(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user1@slac.stanford.edu"),
                        resourceAppId.getPayload()
                )
        );
        assertThat(notFoundException).isNotNull();
        assertThat(notFoundException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createAppFailsWithNotRoot() {
        var notAuthorizedException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.applicationControllerCreateNewApplication(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user2@slac.stanford.edu"),
                        NewApplicationDTO.builder()
                                .name("app1")
                                .expiration(LocalDate.of(2100, 1, 1))
                                .build()
                )
        );
        assertThat(notAuthorizedException).isNotNull();
    }

    @Test
    public void testSearchIntoAllTokens() {
        // create 100 apps
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            assertDoesNotThrow(
                    () -> testControllerHelperService.applicationControllerCreateNewApplication(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            NewApplicationDTO.builder()
                                    .name("app-%02d".formatted(finalI))
                                    .expiration(LocalDate.of(2100, 1, 1))
                                    .build()
                    )
            );
        }

        // search for the first 10
        var searchResult = assertDoesNotThrow(
                () -> testControllerHelperService.applicationControllerFindAllApplication(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        );
        assertThat(searchResult).isNotNull();
        assertThat(searchResult.getPayload()).isNotNull().hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(searchResult.getPayload().get(i).name()).isEqualTo("app-%02d".formatted(i));
        }
    }
}
