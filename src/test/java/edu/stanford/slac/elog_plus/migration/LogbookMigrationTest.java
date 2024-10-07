package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.migration.M007_FixNullOnLogbookReadAWriteAll;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
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
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.ActiveProfiles;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class LogbookMigrationTest {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private LogbookRepository logbookRepository;

    @BeforeEach
    public void clean() {
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    @Test
    public void sanitizeNullReadWriteAll() {
        var newLogbook = assertDoesNotThrow(
                () -> logbookRepository.save(
                        Logbook
                                .builder()
                                .name("logbook-4")
                                .tags(emptyList())
                                .shifts(emptyList())
                                .build()
                )
        );
        Update u = new Update();
        u.unset("readAll");
        u.unset("writeAll");
        mongoTemplate.updateMulti(
                new Query(),
                u,
                Logbook.class
        );

        // run sanitization migration task
        var sanitizationTask = new M007_FixNullOnLogbookReadAWriteAll(mongoTemplate);
        assertDoesNotThrow(sanitizationTask::changeSet);

        // check with default value
        var writeALLShouldBeEmpty = logbookRepository.findAllByWriteAllIsTrue();
        assertThat(writeALLShouldBeEmpty).isNotEmpty();
        var readAllShouldBeEmpty = logbookRepository.findAllByReadAllIsTrue();
        assertThat(readAllShouldBeEmpty).isNotEmpty();
    }
}
