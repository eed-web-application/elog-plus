package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonQueryParameterDTO;
import edu.stanford.slac.ad.eed.baselib.model.Person;
import edu.stanford.slac.ad.eed.baselib.repository.PersonAttributesMapper;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.ldap.query.LdapQueryBuilder.query;

//@AutoConfigureMockMvc
//@SpringBootTest()
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ExtendWith(MockitoExtension.class)
//@ActiveProfiles(profiles = "test-ad-ldap")
public class PeopleGroupServiceTest {
//    @Autowired
//    LdapTemplate ldapTemplate;
//    @MockBean
//    AuthService authService;
//    @Autowired
//    PeopleGroupService peopleGroupService;
//
////    @Test
//    public void findPeopleTest() {
//        String base = "dc=win,dc=slac,dc=stanford,dc=edu";
//        String filter = "(&(objectClass=user)(mail=mgibbs@slac.stanford.edu))";
//        List<String> distinguishedNames = this.ldapTemplate.search(
//                query().where("objectCategory").is("user").and(
//                        query().where("sAMAccountName").is("bisegni")
//                                .or(query().where("userPrincipalName").is("bisegni"))
//                ),
//                (AttributesMapper<String>) attrs -> {
//                    String dn = attrs.get("distinguishedName").get().toString();
//                    // Handle any special characters or encoding issues here
//                    return dn.replace("\\", ""); // Example: Fix any escaping issues
//                }
//        );
//
//        AndFilter filterObject = new AndFilter();
//        filterObject.and(new EqualsFilter("objectCategory", "user"));
////        filterObject.and(new EqualsFilter("objectCategory", "person"));
//        filterObject.and(new EqualsFilter("mail", "bisegni@slac.stanford.edu"));
//        var baseCriteria = query()
//                .filter(filterObject);
//
//        List<Person> result = ldapTemplate.search(baseCriteria, new PersonAttributesMapper());
//
//        System.out.println("Result: " + result);
//        var person = peopleGroupService.findPersonByEMail("bisegni@slac.stanford.edu");
//        assertThat(person).isNotNull();
//        person = peopleGroupService.findPersonByUid("bisegni");
//        assertThat(person).isNotNull();
//        var persons = peopleGroupService.findPersons("bisegni");
//        assertThat(persons).isNotNull();
//
//        var foundPeoplesFirstPage = peopleGroupService.findPersons(
//                PersonQueryParameterDTO
//                        .builder()
//                        .limit(10)
//                        .build()
//        );
//        assertThat(foundPeoplesFirstPage).isNotNull().hasSize(10);
//
//        var foundPeoplesSecondPage = peopleGroupService.findPersons(
//                PersonQueryParameterDTO
//                        .builder()
//                        .anchor(foundPeoplesFirstPage.get(9).uid())
//                        .limit(10)
//                        .build()
//        );
//        assertThat(foundPeoplesSecondPage).isNotNull().hasSize(10);
//    }
}
