package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.migration.M008_RenameSupersedeBy;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.Vector;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class EntryMigrationTest {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    EntryRepository entryRepository;

    class WrongNameEntry extends Entry {
        public String supersedeBy;
    }

    @BeforeEach
    public void clean() {
        mongoTemplate.remove(new Query(), Entry.class);
    }

    @Test
    public void testRenameSupersedeBy() {
        Vector<String> entryIdWithSupersedeBy = new Vector<>();
        // create 100 fake entry some with supersedeBy
        for (int i = 0; i < 100; i++) {
            boolean hasSupersedeBy = i%2==0;
            WrongNameEntry entry = new WrongNameEntry();
            entry.supersedeBy = hasSupersedeBy?"fake":null;
            var savedEntry = entryRepository.save(
                    entry
            );
            if(hasSupersedeBy){
                entryIdWithSupersedeBy.add(savedEntry.getId());
            }
        }

        // run the migration
        assertDoesNotThrow(
                () -> new M008_RenameSupersedeBy(mongoTemplate).changeSet()
        );

        // now all the entry with the supersedeBy field should be renamed

        entryRepository.findAll().forEach(entry -> {
            if(entryIdWithSupersedeBy.contains(entry.getId())){
                assertThat(entry.getSupersededBy()).isNotNull();
            }else{
                assertThat(entry.getSupersededBy()).isNull();
            }
        });
    }
}
