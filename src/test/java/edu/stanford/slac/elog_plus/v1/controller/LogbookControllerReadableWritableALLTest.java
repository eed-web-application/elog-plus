package edu.stanford.slac.elog_plus.v1.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v2.dto.NewLocalGroupDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.LocalGroup;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.apache.kafka.clients.admin.AdminClient;
import org.assertj.core.api.Condition;
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
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LogbookControllerReadableWritableALLTest {
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
    @Autowired
    AdminClient adminClient;

    @BeforeEach
    public void preTest() {
        adminClient.deleteTopics(List.of("elog-plus-import-entry"));
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Entry.class);
        //reset authorizations
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), LocalGroup.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
    }

    @Test
    public void testGetAllLogbookForAuthType() {
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
        // create logbook 2
        var newLogbookApiResultTwo = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of(
                        "user1@slac.stanford.edu"
                ),
                "new logbook 2",
                emptyList()
        );
        // make logbook 2 writable to all
        var updateLogbook2WriteAll = assertDoesNotThrow(
                () -> testControllerHelperService.updateLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookApiResultTwo.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name("new-logbook-2")
                                .tags(emptyList())
                                .shifts(emptyList())
                                .writeAll(true)
                                .build()
                )
        );
        assertThat(updateLogbook2WriteAll).isNotNull();
        assertThat(updateLogbook2WriteAll.getPayload()).isTrue();

        // create logbook 3
        var newLogbookApiResultThree = testControllerHelperService.getNewLogbookWithNameWithAuthorization(
                mockMvc,
                Optional.of("user1@slac.stanford.edu"),
                "new logbook 3",
                emptyList()
        );


        // make logbook 3 readable to all
        var updateLogbook3ReadAll = assertDoesNotThrow(
                () -> testControllerHelperService.updateLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu"),
                        newLogbookApiResultThree.getPayload(),
                        UpdateLogbookDTO
                                .builder()
                                .name("new-logbook-3")
                                .tags(emptyList())
                                .shifts(emptyList())
                                .readAll(true)
                                .build()
                )
        );
        assertThat(updateLogbook3ReadAll).isNotNull();
        assertThat(updateLogbook3ReadAll.getPayload()).isTrue();

        // get readable logbook for user 2
        var allReadableLogbookForUser2 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of(Read.toString())
                )
        );
        assertThat(allReadableLogbookForUser2).isNotNull();
        assertThat(allReadableLogbookForUser2.getPayload()).hasSize(3);
        assertThat(allReadableLogbookForUser2.getPayload())
                .extracting(LogbookDTO::id)
                .contains(
                newLogbookApiResultOne.getPayload(),
                newLogbookApiResultTwo.getPayload(),
                newLogbookApiResultThree.getPayload()
        );

        // get writable logbook for user 2
        var allWritableLogbookForUser2 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of(Write.toString())
                )
        );
        assertThat(allWritableLogbookForUser2).isNotNull();
        assertThat(allWritableLogbookForUser2.getPayload()).hasSize(2);
        assertThat(allWritableLogbookForUser2.getPayload())
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload(),
                        newLogbookApiResultTwo.getPayload()
                );

        // all logbook readable for user 3
        var allReadableLogbookForUser3 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of(Read.toString())
                )
        );
        assertThat(allReadableLogbookForUser3).isNotNull();
        assertThat(allReadableLogbookForUser3.getPayload()).hasSize(3);
        assertThat(allReadableLogbookForUser3.getPayload())
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultOne.getPayload(),
                        newLogbookApiResultTwo.getPayload(),
                        newLogbookApiResultThree.getPayload()
                );

        //get writable logbook for user 3
        var allWritableLogbookForUser3 = assertDoesNotThrow(
                () -> testControllerHelperService.getAllLogbook(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user3@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.of(Write.toString())
                )
        );
        assertThat(allWritableLogbookForUser3).isNotNull();
        assertThat(allWritableLogbookForUser3.getPayload()).hasSize(1);
        assertThat(allWritableLogbookForUser3.getPayload())
                .extracting(LogbookDTO::id)
                .contains(
                        newLogbookApiResultTwo.getPayload()
                );
    }
}
