package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import edu.stanford.slac.elog_plus.service.AuthService;
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

import java.util.List;

import static edu.stanford.slac.elog_plus.model.Authorization.Type.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    public void findAuthorizationByLevel() {
        appProperties.getRootUserList().clear();
        AuthorizationDTO newAuthReadUser2 = assertDoesNotThrow(
                () -> authService.saveAuthorization(
                        AuthorizationDTO.builder()
                                .authorizationType(Read.name())
                                .owner("user2@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        AuthorizationDTO newAuthWriteUser3 = assertDoesNotThrow(
                () -> authService.saveAuthorization(
                        AuthorizationDTO.builder()
                                .authorizationType(Write.name())
                                .owner("user3@slac.stanford.edu")
                                .resource("/r1")
                                .build()
                )
        );

        AuthorizationDTO newAuthAdminUser4 = assertDoesNotThrow(
                () -> authService.saveAuthorization(
                        AuthorizationDTO.builder()
                                .authorizationType(Admin.name())
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

}
