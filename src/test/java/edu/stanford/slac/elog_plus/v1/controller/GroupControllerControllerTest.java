package edu.stanford.slac.elog_plus.v1.controller;


import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.AuthorizationGroupManagementDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.UpdateLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.UserDetailsDTO;
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

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
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
public class GroupControllerControllerTest {
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
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        mongoTemplate.remove(new Query(), LocalGroup.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void createGroup() {
        var groupCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(groupCreationResult).isNotNull();
        assertThat(groupCreationResult.getPayload()).isNotEmpty();

        // fetch group by id
        var fullGroupDetails = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerFindGroupById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        groupCreationResult.getPayload(),
                        Optional.of(true),
                        Optional.empty()
                )
        );
        assertThat(fullGroupDetails).isNotNull();
        assertThat(fullGroupDetails.getPayload()).isNotNull();
        assertThat(fullGroupDetails.getPayload().id()).isEqualTo(groupCreationResult.getPayload());
        assertThat(fullGroupDetails.getPayload().name()).isEqualTo("local-group-1");
        assertThat(fullGroupDetails.getPayload().description()).isEqualTo("local-group-1 description");
        assertThat(fullGroupDetails.getPayload().members())
                .extracting(UserDetailsDTO::email)
                .contains("user2@slac.stanford.edu");

    }

    @Test
    public void deleteGroup() {
        var groupCreationResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(groupCreationResult).isNotNull();
        assertThat(groupCreationResult.getPayload()).isNotEmpty();

        // delete the group
        var deleteGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerDeleteGroup(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        groupCreationResult.getPayload()
                )
        );
        assertThat(deleteGroupResult).isNotNull();
        assertThat(deleteGroupResult.getPayload()).isTrue();

        var groupNotFound = assertThrows(
                ControllerLogicException.class,
                () -> testControllerHelperService.groupControllerFindGroupById(
                        mockMvc,
                        status().is5xxServerError(),
                        Optional.of("user1@slac.stanford.edu"),
                        groupCreationResult.getPayload(),
                        Optional.of(true),
                        Optional.empty()
                )
        );
        assertThat(groupNotFound).isNotNull();
    }

    @Test
    public void createGroupFailsWithNoAuthorization() {
        var notAuthorizedException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user2@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(notAuthorizedException).isNotNull();
    }

    @Test
    public void updateGroupOk() {
        var newGroupIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupIdResult).isNotNull();
        assertThat(newGroupIdResult.getPayload()).isNotEmpty();

        // update group
        var updateGroupResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerUpdateGroup(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newGroupIdResult.getPayload(),
                        UpdateLocalGroupDTO
                                .builder()
                                .name("local-group-1-updated")
                                .description("local-group-1 description updated")
                                .members(List.of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(updateGroupResult).isNotNull();
        assertThat(updateGroupResult.getPayload()).isTrue();

        // fetch full group details
        var fullGroupDetails = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerFindGroupById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanofrd.edu"),
                        newGroupIdResult.getPayload(),
                        Optional.of(true),
                        Optional.empty()
                )
        );
        assertThat(fullGroupDetails).isNotNull();
        assertThat(fullGroupDetails.getPayload()).isNotNull();
        assertThat(fullGroupDetails.getPayload().id()).isEqualTo(newGroupIdResult.getPayload());
        assertThat(fullGroupDetails.getPayload().name()).isEqualTo("local-group-1-updated");
        assertThat(fullGroupDetails.getPayload().description()).isEqualTo("local-group-1 description updated");
        assertThat(fullGroupDetails.getPayload().members())
                .extracting(UserDetailsDTO::email)
                .contains("user2@slac.stanford.edu", "user3@slac.stanford.edu");
    }

    @Test
    public void updateGroupFailsWithUnauthorizedUser() {
        var newGroupIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupIdResult).isNotNull();
        assertThat(newGroupIdResult.getPayload()).isNotEmpty();

        // update group
        var notAuthorizedException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.groupControllerUpdateGroup(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user2@slac.stanford.edu"),
                        newGroupIdResult.getPayload(),
                        UpdateLocalGroupDTO
                                .builder()
                                .name("local-group-1-updated")
                                .description("local-group-1 description updated")
                                .members(List.of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(notAuthorizedException).isNotNull();
    }

    @Test
    public void authorizeUserToManageGroup() {
        // authorize user 2 to manage group
        var authorizeUserResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerManageAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        AuthorizationGroupManagementDTO
                                .builder()
                                .addUsers(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(authorizeUserResult).isNotNull();
        assertThat(authorizeUserResult.getPayload()).isTrue();

        // create group with authorized user
        var newGroupIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user2@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupIdResult).isNotNull();
        assertThat(newGroupIdResult.getPayload()).isNotEmpty();

        // update group with authorized user
        var updateResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerUpdateGroup(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        newGroupIdResult.getPayload(),
                        UpdateLocalGroupDTO
                                .builder()
                                .name("local-group-1-updated")
                                .description("local-group-1 description updated")
                                .members(List.of("user2@slac.stanford.edu", "user3@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getPayload()).isTrue();

        // remove user2 authorization
        var removeUserAuthorizationResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerManageAuthorization(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        AuthorizationGroupManagementDTO
                                .builder()
                                .removeUsers(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(removeUserAuthorizationResult).isNotNull();
        assertThat(removeUserAuthorizationResult.getPayload()).isTrue();

        // update group gives error
        var notAuthorizedException = assertThrows(
                NotAuthorized.class,
                () -> testControllerHelperService.groupControllerUpdateGroup(
                        mockMvc,
                        status().is4xxClientError(),
                        Optional.of("user2@slac.stanford.edu"),
                        newGroupIdResult.getPayload(),
                        UpdateLocalGroupDTO
                                .builder()
                                .name("local-group-1-updated 2")
                                .description("local-group-1 description updated 2")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(notAuthorizedException).isNotNull();
    }

    @Test
    public void findAllTest() {
        // create 100 group
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            var groupCreationResult = assertDoesNotThrow(
                    () -> testControllerHelperService.groupControllerCreateNewGroup(
                            mockMvc,
                            status().isCreated(),
                            Optional.of("user1@slac.stanford.edu"),
                            NewLocalGroupDTO
                                    .builder()
                                    .name("local-group-%02d".formatted(finalI))
                                    .description("local-group-%02d description".formatted(finalI))
                                    .build()
                    )
            );
        }

        // fetch first 10 groups
        var first10Groups = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerFindGroups(
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
        assertThat(first10Groups).isNotNull();
        assertThat(first10Groups.getPayload()).isNotNull();
        assertThat(first10Groups.getPayload()).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(first10Groups.getPayload().get(i).name()).isEqualTo("local-group-%02d".formatted(i));
        }
    }

    @Test
    public void checkLabelOnGroupDetails(){
        var newGroupIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerCreateNewGroup(
                        mockMvc,
                        status().isCreated(),
                        Optional.of("user1@slac.stanford.edu"),
                        NewLocalGroupDTO
                                .builder()
                                .name("local-group-1")
                                .description("local-group-1 description")
                                .members(List.of("user2@slac.stanford.edu"))
                                .build()
                )
        );
        assertThat(newGroupIdResult).isNotNull();
        assertThat(newGroupIdResult.getPayload()).isNotEmpty();

        var newLogbookApiResultOne = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook",
                List.of(
                        NewAuthorizationDTO
                                .builder()
                                .ownerId(newGroupIdResult.getPayload())
                                .ownerType(AuthorizationOwnerTypeDTO.Group)
                                .permission(Write)
                                .build()
                )
        );

        var foundGroup = assertDoesNotThrow(
                () -> testControllerHelperService.groupControllerFindGroupById(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newGroupIdResult.getPayload(),
                        Optional.of(true),
                        Optional.of(true)
                )
        );
        assertThat(foundGroup).isNotNull();
        assertThat(foundGroup.getPayload().authorizations()).hasSize(1);
        assertThat(foundGroup.getPayload().authorizations().get(0).resourceName()).isEqualTo("new-logbook");
    }
}
