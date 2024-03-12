package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
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

import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.*;
import static edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType.Group;
import static edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType.User;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthorizationLogicTest {
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Authorization.class);
    }


    @Test
    public void authorizationEnumTest() {
        Authorization.Type type = Authorization.Type.valueOf("Read");
        assertThat(type).isEqualTo(Authorization.Type.Read);
    }

    @Test
    public void authorizationEnumIntegerTest() {
        Integer type = Authorization.Type.valueOf("Read").getValue();
        assertThat(type).isEqualTo(Authorization.Type.Read.getValue());
    }

    @Test
    public void findUserAuthorizationsInheritedByGroups() {
        //read ->r1
        Authorization newAuthWriteUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(User)
                                .resource("/r1")
                                .build()
                )
        );
        // write->r1
        Authorization newAuthWriteGroups1_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r1")
                                .build()
                )
        );
        // write -> r2
        Authorization newAuthWriteGroups1_2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r2")
                                .build()
                )
        );
        // read -> r3
        Authorization newAuthWriteGroups1_3 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("group-1")
                                .ownerType(Group)
                                .resource("/r3")
                                .build()
                )
        );
        // admin -> r3
        Authorization newAuthWriteGroups2_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("group-2")
                                .ownerType(Group)
                                .resource("/r3")
                                .build()
                )
        );

        List<AuthorizationDTO> allReadAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                Read,
                "/r",
                Optional.empty()
        );

        // check auth on r1 read|write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r2 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r2") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r3 read|admin
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r3") == 0)
                .filteredOn(
                        auth ->
                                auth.authorizationType() == Read ||
                                        auth.authorizationType() == Admin
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1", "group-2");

        // remove all the authorization from the same resource and keep all the higher one
        List<AuthorizationDTO> allHigherAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                Read,
                "/r",
                Optional.of(true)
        );

        // check auth on r1 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r2 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r2") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Write
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r3 admin
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r3") == 0)
                .filteredOn(
                        auth -> auth.authorizationType() == Admin
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-2");
    }


    @Test
    public void findAuthorizationByLevel() {
        appProperties.getRootUserList().clear();
        Authorization newAuthReadUser2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthWriteUser3 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Write.getValue())
                                .owner("user3@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthAdminUser4 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Authorization.Type.Admin.getValue())
                                .owner("user4@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        //get all the reader
        List<Authorization> readerShouldBeAllUser = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Authorization.Type.Read.getValue()
                )
        );

        assertThat(readerShouldBeAllUser).hasSize(3);
        assertThat(readerShouldBeAllUser)
                .extracting(Authorization::getOwner)
                .contains(
                        "user2@slac.stanford.edu",
                        "user3@slac.stanford.edu",
                        "user4@slac.stanford.edu"
                );

        //get all the writer
        List<Authorization> writerShouldBeUser3And4 = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Authorization.Type.Write.getValue()
                )
        );

        assertThat(writerShouldBeUser3And4).hasSize(2);
        assertThat(writerShouldBeUser3And4)
                .extracting(Authorization::getOwner)
                .contains(
                        "user3@slac.stanford.edu",
                        "user4@slac.stanford.edu"
                );

        //get all the writer
        List<Authorization> adminShouldBeUser4 = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Authorization.Type.Admin.getValue()
                )
        );

        assertThat(adminShouldBeUser4).hasSize(1);
        assertThat(adminShouldBeUser4)
                .extracting(Authorization::getOwner)
                .contains(
                        "user4@slac.stanford.edu"
                );
    }

    static private class AuthorizationIs extends Condition<String> {

        private final String expectedValue;

        private AuthorizationIs(String expectedValue) {
            this.expectedValue = expectedValue;
        }

        public static AuthorizationIs ofType(String type) {
            return new AuthorizationIs(type);
        }

        @Override
        public boolean matches(String actualValue) {
            return actualValue != null && actualValue.compareToIgnoreCase(expectedValue) == 0;
        }
    }
}
