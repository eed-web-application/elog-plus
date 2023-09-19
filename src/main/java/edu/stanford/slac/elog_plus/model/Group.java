package edu.stanford.slac.elog_plus.model;

import lombok.Data;
import org.springframework.ldap.odm.annotations.Attribute;
import org.springframework.ldap.odm.annotations.Entry;
import org.springframework.ldap.odm.annotations.Id;

import javax.naming.Name;
import java.util.List;

@Entry(
        base = "ou=Group",
        objectClasses = { "posixGroup" }
)
@Data
public final class Group {
    @Id
    private Name id;
    private @Attribute(name = "cn") String commonName;
    private @Attribute(name = "memberUid") List<String> memberUid;

}
