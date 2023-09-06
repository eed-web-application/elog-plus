package edu.stanford.slac.elog_plus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.ldap.repository.config.EnableLdapRepositories;

@Configuration
@EnableLdapRepositories(basePackages = "edu.stanford.slac.elog_plus.ldap_repository")
public class InitLDAP {
}
