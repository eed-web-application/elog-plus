package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
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

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class LogbookServiceTest {
    @Autowired
    private LogbookService logbookService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    @Test
    public void createNew() {
        String newID = assertDoesNotThrow(
                () -> logbookService.createNew(
                        NewLogbookDTO
                                .builder()
                                .name("new-logbook")
                                .build()
                )
        );

        assertThat(newID).isNotNull().isNotEmpty();
    }

    @Test
    public void fetchAll() {
        String newID = assertDoesNotThrow(
                () -> logbookService.createNew(
                        NewLogbookDTO
                                .builder()
                                .name("new-logbook")
                                .build()
                )
        );

        assertThat(newID).isNotNull().isNotEmpty();

        List<LogbookDTO> allLogbook = assertDoesNotThrow(
                () -> logbookService.getAllLogbook()
        );

        assertThat(allLogbook).isNotNull().isNotEmpty();
    }
}
