package edu.stanford.slac.elog_plus.v1.controller;


import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ResourceTypeDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UpdateAuthorizationDTO;
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
public class AuthorizationControllerControllerTest {
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
    public void testCreateRooUser() {
        assertDoesNotThrow(
                () -> testControllerHelperService.authorizationControllerCreateNewAuthorization(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .resourceType(ResourceTypeDTO.All)
                                .resourceId("*")
                                .authorizationType(AuthorizationTypeDTO.Admin)
                                .build()
                )
        );

        // fetch user details
        var userDetailsResult = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindUserById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        "user2@slac.stanford.edu",
                        Optional.of(true),
                        Optional.of(false)
                )
        );
        assertThat(userDetailsResult).isNotNull();
        assertThat(userDetailsResult.getPayload()).isNotNull();
        assertThat(userDetailsResult.getPayload().email()).isEqualTo("user2@slac.stanford.edu");
        assertThat(userDetailsResult.getPayload().isRoot()).isTrue();

        // find root authorization
        var rootAuthorization = userDetailsResult.getPayload().authorization()
                .stream()
                .filter
                        (
                                authorization -> authorization.permission() == AuthorizationTypeDTO.Admin &&
                                        authorization.resourceType() == ResourceTypeDTO.All
                        )
                .findFirst();
        assertThat(rootAuthorization).isPresent();
        // remove as user
        var deleteRootAuthorizationResult = assertDoesNotThrow(
                () -> testControllerHelperService.authorizationControllerDeleteAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        rootAuthorization.get().id()
                )
        );
        assertThat(deleteRootAuthorizationResult).isNotNull();
        assertThat(deleteRootAuthorizationResult.getPayload()).isTrue();

        var userDetailsNoRootResult = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindUserById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        "user2@slac.stanford.edu",
                        Optional.of(true),
                        Optional.of(false)
                )
        );
        assertThat(userDetailsNoRootResult).isNotNull();
        assertThat(userDetailsNoRootResult.getPayload()).isNotNull();
        assertThat(userDetailsNoRootResult.getPayload().email()).isEqualTo("user2@slac.stanford.edu");
        assertThat(userDetailsNoRootResult.getPayload().isRoot()).isFalse();
    }

    @Test
    public void updateAuthorization() {
        var newAuthIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.authorizationControllerCreateNewAuthorization(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .resourceType(ResourceTypeDTO.Logbook)
                                .resourceId("1")
                                .authorizationType(AuthorizationTypeDTO.Admin)
                                .build()
                )
        );

        // fetch user details
        var userDetailsResult = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindUserById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        "user2@slac.stanford.edu",
                        Optional.of(true),
                        Optional.of(false)
                )
        );
        assertThat(userDetailsResult).isNotNull();
        assertThat(userDetailsResult.getPayload()).isNotNull();
        assertThat(userDetailsResult.getPayload().email()).isEqualTo("user2@slac.stanford.edu");
        assertThat(userDetailsResult.getPayload().authorization())
                .hasSize(1)
                .anySatisfy(
                        authorization -> {
                            assertThat(authorization.permission()).isEqualTo(AuthorizationTypeDTO.Admin);
                            assertThat(authorization.resourceType()).isEqualTo(ResourceTypeDTO.Logbook);
                            assertThat(authorization.resourceId()).isEqualTo("1");
                        }
                );

        assertDoesNotThrow(
                () -> testControllerHelperService.authorizationControllerUpdateAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        userDetailsResult.getPayload().authorization().get(0).id(),
                        UpdateAuthorizationDTO
                                .builder()
                                .permission(AuthorizationTypeDTO.Write)
                                .build()

                )
        );
        var userDetailsUpdatedResult = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindUserById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        "user2@slac.stanford.edu",
                        Optional.of(true),
                        Optional.of(false)
                )
        );
        assertThat(userDetailsUpdatedResult).isNotNull();
        assertThat(userDetailsUpdatedResult.getPayload()).isNotNull();
        assertThat(userDetailsUpdatedResult.getPayload().email()).isEqualTo("user2@slac.stanford.edu");
        assertThat(userDetailsUpdatedResult.getPayload().authorization())
                .hasSize(1)
                .anySatisfy(
                        authorization -> {
                            assertThat(authorization.permission()).isEqualTo(AuthorizationTypeDTO.Write);
                            assertThat(authorization.resourceType()).isEqualTo(ResourceTypeDTO.Logbook);
                            assertThat(authorization.resourceId()).isEqualTo("1");
                        }
                );
    }
}
