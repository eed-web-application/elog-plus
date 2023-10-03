package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.exception.AuthenticationTokenMalformed;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.repository.AuthenticationTokenRepository;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import edu.stanford.slac.elog_plus.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.model.Authorization.Type.Read;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest(properties = {})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
public class AuthServiceTokenTest {
    @Autowired
    AppProperties appProperties;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @SpyBean
    @Autowired
    private AuthenticationTokenRepository authenticationTokenRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
    }

    @Test
    public void createTokenFailsOnMalformed() {
        AuthenticationTokenMalformed malformedException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .build()
                )
        );

        assertThat(malformedException.getErrorCode()).isEqualTo(-1);

        malformedException = assertThrows(
                AuthenticationTokenMalformed.class,
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("is the name")
                                .build()
                )
        );
        assertThat(malformedException.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void createTokenOk() {
        AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );

        assertThat(newAuthToken).isNotNull();
        assertThat(newAuthToken.id()).isNotNull().isNotEmpty();
    }

    @Test
    public void createTokenGetOkByIDAndName() {
        AuthenticationTokenDTO newAuthToken = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );

        assertThat(newAuthToken).isNotNull();
        assertThat(newAuthToken.id()).isNotNull().isNotEmpty();

        Optional<AuthenticationTokenDTO> tokenByID = assertDoesNotThrow(
                () -> authService.getAuthenticationTokenById(
                        newAuthToken.id()
                )
        );
        assertThat(tokenByID.isPresent()).isTrue();
        assertThat(tokenByID.get().id()).isEqualTo(newAuthToken.id());

        Optional<AuthenticationTokenDTO> tokenByName = assertDoesNotThrow(
                () -> authService.getAuthenticationTokenByName(
                        "token-a"
                )
        );
        assertThat(tokenByName.isPresent()).isTrue();
        assertThat(tokenByName.get().id()).isEqualTo(newAuthToken.id());
    }

    @Test
    public void createTokenGetOkGetAllOk() {
        AuthenticationTokenDTO newAuthToken1 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        AuthenticationTokenDTO newAuthToken2 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-b")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        List<AuthenticationTokenDTO> allTokens = assertDoesNotThrow(
                () -> authService.getAllAuthenticationToken()
        );
        assertThat(allTokens)
                .hasSize(2)
                .extracting(AuthenticationTokenDTO::name)
                .containsExactly(
                        "token-a",
                        "token-b"
                );
    }

    /**
     * The deletion of the token delete also the authorization
     */
    @Test
    public void deleteTokenDeleteAlsoAuthorization() {
        AuthenticationTokenDTO newAuthToken1 = assertDoesNotThrow(
                () -> authService.addNewAuthenticationToken(
                        NewAuthenticationTokenDTO
                                .builder()
                                .name("token-a")
                                .expiration(LocalDate.of(2023,12,31))
                                .build()
                )
        );
        // add authorization
        Authorization newAuth = assertDoesNotThrow(
                () -> authorizationRepository.save(
                        Authorization
                                .builder()
                                .authorizationType(Read.getValue())
                                .resource("r1")
                                .owner(newAuthToken1.email())
                                .ownerType(Authorization.OType.Application)
                                .build()
                )
        );
        //delete token
        assertDoesNotThrow(
                () -> authService.deleteToken(
                        newAuthToken1.id()
                )
        );

        // now the authorization should be gone away
        var exists = assertDoesNotThrow(()->authorizationRepository.existsById(newAuth.getId()));
        assertThat(exists).isFalse();
    }

//    /**
//     * Here is simulated the exception during to delete of the token and in this way the
//     * authorization, that are deleted before it will not be erased due to transaction abortion
//     */
//    @Test
//    public void testExceptionOnDeletingTokenNotDeleteAuth(){
//        // throw exception during the delete operation of the authentication token
//        AuthenticationTokenDTO newAuthToken1 = assertDoesNotThrow(
//                () -> authService.addNewAuthenticationToken(
//                        NewAuthenticationTokenDTO
//                                .builder()
//                                .name("token-a")
//                                .expiration(LocalDate.of(2023,12,31))
//                                .build()
//                )
//        );
//        // add authorization
//        Authorization newAuth = assertDoesNotThrow(
//                () -> authorizationRepository.save(
//                        Authorization
//                                .builder()
//                                .authorizationType(Read.getValue())
//                                .resource("r1")
//                                .owner(newAuthToken1.email())
//                                .ownerType(Authorization.OType.Application)
//                                .build()
//                )
//        );
//        Mockito.doThrow(new RuntimeException()).when(authenticationTokenRepository).deleteById(
//                Mockito.any(String.class)
//        );
//        //delete token
//        ControllerLogicException deleteException = assertThrows(
//                ControllerLogicException.class,
//                () -> authService.deleteToken(
//                        newAuthToken1.id()
//                )
//        );
//        assertThat(deleteException.getErrorCode()).isEqualTo(-3);
//        // now the authorization should be gone away
//        var exists = assertDoesNotThrow(()->authorizationRepository.existsById(newAuth.getId()));
//        assertThat(exists).isTrue();
//    }
}
