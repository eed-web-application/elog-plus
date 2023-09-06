package edu.stanford.slac.elog_plus.model;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;

@Entry(
        base = "ou=People",
        objectClasses = { "person" }
)
@Data
public final class Person {
    @Id
    private Name id;
    private String uid;
    private String mail;
    private @Attribute(name = "cn") String commonName;
    private @Attribute(name = "sn") String surname;
}
