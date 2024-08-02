package edu.stanford.slac.elog_plus.v1.controller;


import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
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

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserControllerControllerTest {
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
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void testFindAllUsers() {
        var newLogbook1result = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 1",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Write
                                )
                                .build(),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user3@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Read
                                )
                                .build()
                )
        );
        assertThat(newLogbook1result).isNotNull();
        var newLogbook2result = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 2",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user1@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Read
                                )
                                .build(),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Write
                                )
                                .build(),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user3@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Admin
                                )
                                .build()
                )
        );
        assertThat(newLogbook1result).isNotNull();

        var foundUsers = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindAllUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true),
                        Optional.of(true)
                )
        );
        assertThat(foundUsers).isNotNull();
        assertThat(foundUsers.getPayload()).hasSize(3);
        assertThat(foundUsers.getPayload())
                .extracting(UserDetailsDTO::email)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu", "user3@slac.stanford.edu");
        assertThat(foundUsers.getPayload().get(0).authorizations()).hasSize(2);
        assertThat(foundUsers.getPayload().get(0).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Read.name(), newLogbook2result.getPayload(), "Logbook"),
                        tuple(Admin.name(), null, "All")
                );
        assertThat(foundUsers.getPayload().get(1).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Write.name(), newLogbook1result.getPayload(), "Logbook"),
                        tuple(Write.name(), newLogbook2result.getPayload(), "Logbook")
                );
        assertThat(foundUsers.getPayload().get(2).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Read.name(), newLogbook1result.getPayload(), "Logbook"),
                        tuple(Admin.name(), newLogbook2result.getPayload(), "Logbook")
                );

    }

    @Test
    public void updateUserDetails() {
        var newLogbook1result = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 1",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Write
                                )
                                .build(),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user3@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Read
                                )
                                .build()
                )
        );
        assertThat(newLogbook1result).isNotNull();
        var newLogbook2result = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 2",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user3@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(
                                        Admin
                                )
                                .build()
                )
        );
        assertThat(newLogbook2result).isNotNull();

        var foundUsers = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindAllUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true),
                        Optional.of(true)
                )
        );
        assertThat(foundUsers).isNotNull();
        assertThat(foundUsers.getPayload()).hasSize(3);
        assertThat(foundUsers.getPayload())
                .extracting(UserDetailsDTO::email)
                .contains("user1@slac.stanford.edu", "user2@slac.stanford.edu", "user3@slac.stanford.edu");
        assertThat(foundUsers.getPayload().get(0).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Admin.name(), null, "All")
                );
        assertThat(foundUsers.getPayload().get(1).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Write.name(), newLogbook1result.getPayload(), "Logbook")
                );
        assertThat(foundUsers.getPayload().get(2).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Read.name(), newLogbook1result.getPayload(), "Logbook"),
                        tuple(Admin.name(), newLogbook2result.getPayload(), "Logbook")
                );

        var updatedUser2 = assertDoesNotThrow(
                () -> testControllerHelperService.authorizationControllerCreateNewAuthorization(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .resourceId(newLogbook2result.getPayload())
                                .resourceType(ResourceTypeDTO.Logbook)
                                .permission(Read)
                                .build()
                )
        );
        assertThat(updatedUser2).isNotNull();

        var foundUsers2 = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindAllUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true),
                        Optional.of(true)
                )
        );
        assertThat(foundUsers2).isNotNull();
        assertThat(foundUsers2.getPayload()).hasSize(3);
        assertThat(foundUsers2.getPayload().get(1).authorizations())
                .extracting(
                        a -> a.permission().name(),
                        a -> a.resourceId(),
                        a -> a.resourceType().name()
                )
                .contains(
                        tuple(Write.name(), newLogbook1result.getPayload(), "Logbook"),
                        tuple(Read.name(), newLogbook2result.getPayload(), "Logbook")
                );
    }

    @Test
    public void checkLabelOnUserDetails() {
        var newLogbookApiResultOne = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId("user2@slac.stanford.edu")
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .permission(Write)
                                .build()
                )
        );

        var foundUser = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindUserById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        "user2@slac.stanford.edu",
                        Optional.of(true),
                        Optional.of(false)
                )
        );
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getPayload().authorizations()).hasSize(1);
        assertThat(foundUser.getPayload().authorizations().get(0).resourceName()).isEqualTo("new-logbook");
    }

    @Test
    public void getCurrentUserDetails() {
        var currentUserDetails = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindMe(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.empty()
                )
        );

        assertThat(currentUserDetails).isNotNull();
        assertThat(currentUserDetails.getPayload().email()).isNotNull();
        assertThat(currentUserDetails.getPayload().email()).isEqualTo("user1@slac.stanford.edu");
        assertThat(currentUserDetails.getPayload().isRoot()).isTrue();

        currentUserDetails = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindMe(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of("user2@slac.stanford.edu")
                )
        );

        assertThat(currentUserDetails).isNotNull();
        assertThat(currentUserDetails.getPayload().email()).isNotNull();
        assertThat(currentUserDetails.getPayload().email()).isEqualTo("user2@slac.stanford.edu");
        assertThat(currentUserDetails.getPayload().isRoot()).isFalse();
    }

    @Test
    public void testUsersPagination() {
        var firstPage = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindAllUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(true),
                        Optional.of(true)
                )
        );

        assertThat(firstPage).isNotNull();
        assertThat(firstPage.getPayload()).hasSize(10);
        assertThat(firstPage.getPayload().get(0).email()).isEqualTo("user1@slac.stanford.edu");
        assertThat(firstPage.getPayload().get(1).email()).isEqualTo("user10@slac.stanford.edu");
        assertThat(firstPage.getPayload().get(9).email()).isEqualTo("user18@slac.stanford.edu");

        var secondPage = assertDoesNotThrow(
                () -> testControllerHelperService.userControllerFindAllUsers(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.of(firstPage.getPayload().get(9).id()),
                        Optional.empty(),
                        Optional.of(true),
                        Optional.of(true)
                )
        );
        assertThat(secondPage).isNotNull();
        assertThat(secondPage.getPayload()).hasSize(10);
    }

}
