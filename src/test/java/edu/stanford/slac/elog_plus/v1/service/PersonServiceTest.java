package edu.stanford.slac.elog_plus.v1.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class PersonServiceTest {

    @BeforeAll
    public static void setup() throws Exception {
        // Create an embedded LDAP server
//        LdapEmbeddedServerFactory ldapServerFactory = new LdapEmbeddedServerFactory();
//        ldapServerFactory.setPort(8389);
//        ldapServerFactory.setBaseDN("dc=springframework,dc=org");
//        ldapServerFactory.create();
    }
}
