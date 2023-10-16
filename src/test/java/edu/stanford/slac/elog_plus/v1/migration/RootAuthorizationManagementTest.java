package edu.stanford.slac.elog_plus.v1.migration;

import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.migration.MongoDDLOps;
import edu.stanford.slac.elog_plus.migration.RootAuthorizationManagement;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.repository.AuthenticationTokenRepository;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.v1.service.SharedUtilityService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class RootAuthorizationManagementTest {
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoMappingContext mongoMappingContext;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthenticationTokenRepository authenticationTokenRepository;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    private SharedUtilityService sharedUtilityService;
    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
    }
    @Test
    public void testRootInitUserAndTokens() throws IOException {
        appProperties.getRootAuthenticationTokenList().clear();
        appProperties.getRootAuthenticationTokenList().add(
                NewAuthenticationTokenDTO
                        .builder()
                        .name("root-token-1")
                        .expiration(LocalDate.of(2023,1,1))
                        .build()
        );
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("root-user-1@domain.com");

        RootAuthorizationManagement rootAuthorizationManagement = new RootAuthorizationManagement(authService,mongoMappingContext);
        rootAuthorizationManagement.changeSet();

        assertThat(authorizationRepository.findAll()).hasSize(2)
                .extracting(Authorization::getOwner)
                .contains("root-user-1@domain.com",sharedUtilityService.getTokenEmailForGlobalToken("root-token-1"));

        assertThat(authenticationTokenRepository.findAll())
                .extracting(AuthenticationToken::getEmail)
                .contains(sharedUtilityService.getTokenEmailForGlobalToken("root-token-1"));
    }
}
