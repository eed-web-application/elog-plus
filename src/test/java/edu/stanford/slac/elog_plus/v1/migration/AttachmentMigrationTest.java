package edu.stanford.slac.elog_plus.v1.migration;

import edu.stanford.slac.elog_plus.migration.M008_RenameSupersedeBy;
import edu.stanford.slac.elog_plus.migration.M009_CreateAttachmentReferenceInfoIndex;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
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

import java.time.LocalDateTime;
import java.util.Vector;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class AttachmentMigrationTest {
    @Autowired
    MongoTemplate mongoTemplate;
    @Autowired
    AttachmentRepository attachmentRepository;

    static class WrongAttachment extends Attachment {
        public LocalDateTime creationData;
    }

    @BeforeEach
    public void clean() {
        mongoTemplate.remove(new Query(), Attachment.class);
    }

    @Test
    public void testRenameOfCreationDataToCreatedDate() {
        Vector<String> entryIdWithSupersedeBy = new Vector<>();
        // create 100 fake entry some with supersedeBy
        for (int i = 0; i < 100; i++) {
            boolean hasSupersedeBy = i%2==0;
            WrongAttachment att = new WrongAttachment();
            att.creationData = LocalDateTime.now();
            var savedEntry = attachmentRepository.save(
                    att
            );
        }
        // remove the createdDate field for test the rename
        mongoTemplate.getCollection("attachment").updateMany(
                new Query().getQueryObject(),
                new org.bson.Document("$unset", new org.bson.Document("createdDate", ""))
        );
        // run the migration
        assertDoesNotThrow(
                () -> new M009_CreateAttachmentReferenceInfoIndex(mongoTemplate).changeSet()
        );

        // now all the entry with the supersedeBy field should be renamed
        attachmentRepository.findAll().forEach(attachment -> {
            assertThat(attachment.getCreatedDate()).isNotNull();
        });
    }
}
