package edu.stanford.slac.elog_plus.v1.service;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthorizationServiceRootManagementTest {
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
    public void getCreateRootUser() {
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
        List<Authorization> rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu");
    }

    @Test
    public void getUpdateRootUser() {
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();
        List<Authorization> rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu");

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user2@slac.stanford.edu");

        authService.updateRootUser();
        rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(1);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user2@slac.stanford.edu");
    }

    @Test
    public void getUpdateWithRemoveRootUser() {
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu", "user3@slac.stanford.edu"));
        authService.updateRootUser();
        List<Authorization> rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(2);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu", "user3@slac.stanford.edu");

        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().addAll(List.of("user1@slac.stanford.edu", "user2@slac.stanford.edu"));

        authService.updateRootUser();
        rootAuth = authorizationRepository.findByResourceIsAndAuthorizationTypeIsGreaterThanEqual(
                "*",
                Authorization.Type.Admin.getValue()
        );

        assertThat(rootAuth).hasSize(2);
        assertThat(rootAuth).extracting(Authorization::getOwner).contains("user1@slac.stanford.edu", "user2@slac.stanford.edu");
    }
}
