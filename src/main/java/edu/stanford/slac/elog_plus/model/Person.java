package edu.stanford.slac.elog_plus.model;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;

@Entry(
        base = "dc=sdf,dc=slac,dc=stanford,dc=edu",
        objectClasses = { "person" }
)
@Data
public class Person {
    private @Attribute(name = "uid") String uid;
    private @Attribute(name = "cn") String commonName;
    private @Attribute(name = "sn") String surname;
}
