package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PropertySource("classpath:application.yml")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TagControllerTest {
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
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void getTagsNeedToReturnOnlyAuthorizedTags() throws Exception {
        ApiResultResponse<String> creationResult1 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );

        assertThat(creationResult1).isNotNull();
        assertThat(creationResult1.getErrorCode()).isEqualTo(0);
        assertThat(creationResult1.getPayload()).isNotEmpty();

        ApiResultResponse<String> newTagResult1 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewLogbookTags(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        creationResult1.getPayload(),
                        NewTagDTO
                                .builder()
                                .name("new-tag-1")
                                .build()
                )
        );
        assertThat(newTagResult1).isNotNull();
        assertThat(newTagResult1.getErrorCode()).isEqualTo(0);
        assertThat(newTagResult1.getPayload()).isNotEmpty();

        ApiResultResponse<String> creationResult2 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        NewLogbookDTO.builder()
                                .name("new-logbooks-2")
                                .build()
                )
        );

        assertThat(creationResult2).isNotNull();
        assertThat(creationResult2.getErrorCode()).isEqualTo(0);
        assertThat(creationResult2.getPayload()).isNotEmpty();

        ApiResultResponse<String> newTagResult2 = assertDoesNotThrow(
                () -> testControllerHelperService.createNewLogbookTags(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        creationResult2.getPayload(),
                        NewTagDTO
                                .builder()
                                .name("new-tag-2")
                                .build()
                )
        );
        assertThat(newTagResult2).isNotNull();
        assertThat(newTagResult2.getErrorCode()).isEqualTo(0);
        assertThat(newTagResult2.getPayload()).isNotEmpty();

        // with user 1 need to return all tags
        ApiResultResponse<List<TagDTO>> allTagResultForUser1 = assertDoesNotThrow(
                () -> testControllerHelperService.tagControllerGetAllTags(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        Optional.empty()
                )
        );

        assertThat(allTagResultForUser1).isNotNull();
        assertThat(allTagResultForUser1.getErrorCode()).isEqualTo(0);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser1.getPayload()).hasSize(2);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser1.getPayload()).extracting("name").contains("new-tag-1","new-tag-2");

        // authorize user 2 to read only from logbook 2
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
                                .resourceId(creationResult2.getPayload())
                                .permission(AuthorizationTypeDTO.Admin)
                                .build()
                )
        );

        // now user 2 should see only the tag from logbook 2
        ApiResultResponse<List<TagDTO>> allTagResultForUser2 = assertDoesNotThrow(
                () -> testControllerHelperService.tagControllerGetAllTags(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        Optional.empty()
                )
        );

        assertThat(allTagResultForUser2).isNotNull();
        assertThat(allTagResultForUser2.getErrorCode()).isEqualTo(0);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser2.getPayload()).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser2.getPayload()).extracting("name").contains("new-tag-2");

        // try to force the user 2 to see the tag from logbook 1
        ApiResultResponse<List<TagDTO>> allTagResultForUser2OnLogbook1 = assertDoesNotThrow(
                () -> testControllerHelperService.tagControllerGetAllTags(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        Optional.of(List.of(creationResult1.getPayload()))
                )
        );

        assertThat(allTagResultForUser2OnLogbook1).isNotNull();
        assertThat(allTagResultForUser2OnLogbook1.getErrorCode()).isEqualTo(0);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser2OnLogbook1.getPayload()).hasSize(0);

        // force the user 2 to see the tag from logbook 2
        ApiResultResponse<List<TagDTO>> allTagResultForUser2OnLogbook2 = assertDoesNotThrow(
                () -> testControllerHelperService.tagControllerGetAllTags(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user2@slac.stanford.edu"
                        ),
                        Optional.of(List.of(creationResult2.getPayload()))
                )
        );

        assertThat(allTagResultForUser2OnLogbook2).isNotNull();
        assertThat(allTagResultForUser2OnLogbook2.getErrorCode()).isEqualTo(0);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser2OnLogbook2.getPayload()).hasSize(1);
        AssertionsForInterfaceTypes.assertThat(allTagResultForUser2OnLogbook2.getPayload()).extracting("name").contains("new-tag-2");
    }
}
