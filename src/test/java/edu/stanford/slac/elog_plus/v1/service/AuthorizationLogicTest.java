package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationType;
import edu.stanford.slac.elog_plus.api.v1.dto.EntrySummaryDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import edu.stanford.slac.elog_plus.service.AuthService;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.model.Authorization.Type.*;
import static org.assertj.core.api.AssertionsForClassTypes.not;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
public class AuthorizationLogicTest {
    @Autowired
    AppProperties appProperties;
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
        assertThat(type).isEqualTo(Read);
    }

    @Test
    public void authorizationEnumIntegerTest() {
        Integer type = Authorization.Type.valueOf("Read").getValue();
        assertThat(type).isEqualTo(Read.getValue());
    }

    @Test
    public void findUserAuthorizationsInheritedByGroups() {
        //read ->r1
        Authorization newAuthWriteUser1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Read.getValue())
                                .owner("user1@slac.stanford.edu")
                                .ownerType(Authorization.OType.User)
                                .resource("/r1")
                                .build()
                )
        );
        // write->r1
        Authorization newAuthWriteGroups1_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Write.getValue())
                                .owner("group-1")
                                .ownerType(Authorization.OType.Group)
                                .resource("/r1")
                                .build()
                )
        );
        // write -> r2
        Authorization newAuthWriteGroups1_2 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Write.getValue())
                                .owner("group-1")
                                .ownerType(Authorization.OType.Group)
                                .resource("/r2")
                                .build()
                )
        );
        // read -> r3
        Authorization newAuthWriteGroups1_3 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Read.getValue())
                                .owner("group-1")
                                .ownerType(Authorization.OType.Group)
                                .resource("/r3")
                                .build()
                )
        );
        // admin -> r3
        Authorization newAuthWriteGroups2_1 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Admin.getValue())
                                .owner("group-2")
                                .ownerType(Authorization.OType.Group)
                                .resource("/r3")
                                .build()
                )
        );

        List<AuthorizationDTO> allReadAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                AuthorizationType.Read,
                "/r"
        );

        // check auth on r1 read|write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .filteredOn(
                        auth -> auth.authorizationType().compareToIgnoreCase("Write") == 0
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r2 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r2") == 0)
                .filteredOn(
                        auth -> auth.authorizationType().compareToIgnoreCase("Write") == 0
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r3 read|admin
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r3") == 0)
                .filteredOn(
                        auth ->
                                auth.authorizationType().compareToIgnoreCase("Read") == 0 ||
                                        auth.authorizationType().compareToIgnoreCase("Admin") == 0
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1", "group-2");

        // remove all the authorization from the same resource and keep all the higher one
        List<AuthorizationDTO> allHigherAuthorization = authService.getAllAuthorizationForOwnerAndAndAuthTypeAndResourcePrefix(
                "user1@slac.stanford.edu",
                AuthorizationType.Read,
                "/r",
                Optional.of(true)
        );

        // check auth on r1 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r1") == 0)
                .filteredOn(
                        auth -> auth.authorizationType().compareToIgnoreCase("Write") == 0
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r2 write
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r2") == 0)
                .filteredOn(
                        auth -> auth.authorizationType().compareToIgnoreCase("Write") == 0
                )
                .extracting(AuthorizationDTO::owner)
                .contains("group-1");

        // check auth on r3 admin
        assertThat(allReadAuthorization)
                .filteredOn(auth -> auth.resource().compareToIgnoreCase("/r3") == 0)
                .filteredOn(
                        auth -> auth.authorizationType().compareToIgnoreCase("Admin") == 0
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
                                .authorizationType(Read.getValue())
                                .owner("user2@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthWriteUser3 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Write.getValue())
                                .owner("user3@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        Authorization newAuthAdminUser4 = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization.builder()
                                .authorizationType(Admin.getValue())
                                .owner("user4@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        //get all the reader
        List<Authorization> readerShouldBeAllUser = assertDoesNotThrow(
                () -> authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                        "/r1",
                        Read.getValue()
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
                        Write.getValue()
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
                        Admin.getValue()
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
