package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ResourceTypeDTO;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthorizationServiceTest {
    @Autowired
    private SharedUtilityService sharedUtilityService;
    @Autowired
    private AuthorizationServices authorizationServices;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
    }

    @Test
    public void avoidDoubleAuthorizationSameResourceSameUser() {
        assertDoesNotThrow(() ->
                authorizationServices.createNew(
                NewAuthorizationDTO
                        .builder()
                        .permission(AuthorizationTypeDTO.Read)
                        .resourceId("r1")
                        .resourceType(ResourceTypeDTO.Logbook)
                        .ownerType(AuthorizationOwnerTypeDTO.User)
                        .ownerId("user1@slac.stanford.edu")
                        .build()
                )
        );
        // no throw on same authorization
        assertDoesNotThrow(() -> authorizationServices.createNew(
                        NewAuthorizationDTO
                                .builder()
                                .permission(AuthorizationTypeDTO.Write)
                                .resourceId("r1")
                                .resourceType(ResourceTypeDTO.Logbook)
                                .ownerType(AuthorizationOwnerTypeDTO.User)
                                .ownerId("user1@slac.stanford.edu")
                                .build()
                )
        );
    }
}
