package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.EntryService;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.not;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EntryServiceSuperseedTest {
    @Autowired
    private EntryService entryService;
    @Autowired
    private LogbookService logbookService;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private SharedUtilityService sharedUtilityService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Logbook.class);
    }

    private LogbookDTO getTestLogbook() {
        String logbookId =
                assertDoesNotThrow(
                        () -> logbookService.createNew(
                                NewLogbookDTO
                                        .builder()
                                        .name(UUID.randomUUID().toString())
                                        .build()
                        )
                );
        return assertDoesNotThrow(
                () -> logbookService.getLogbook(logbookId)
        );
    }

    @Test
    public void fixFindAllAfterSuperseedOpsAFollowedUpEntry() {
        // create the logbook
        var logbook = getTestLogbook();
        // create entry
        String newLogID = entryService.createNew(
                EntryNewDTO
                        .builder()
                        .logbooks(List.of(logbook.id()))
                        .text("This is a log for test")
                        .title("A very wonderful log")
                        .build(),
                sharedUtilityService.getPersonForEmail("user1@slac.stanford.edu")
        );

        assertThat(newLogID).isNotNull();

        // create a new followup to the entry
        entryService.createNewFollowUp(
                newLogID,
                EntryNewDTO
                        .builder()
                        .logbooks(List.of(logbook.id()))
                        .text("This is a followup log for log %s".formatted(newLogID))
                        .title("A very wonderful followup log")
                        .build(),
                sharedUtilityService.getPersonForEmail("user1@slac.stanford.edu")
        );

        // create another follow up
        entryService.createNewFollowUp(
                newLogID,
                EntryNewDTO
                        .builder()
                        .logbooks(List.of(logbook.id()))
                        .text("This is another followup log for log %s".formatted(newLogID))
                        .title("A very wonderful followup log")
                        .build(),
                sharedUtilityService.getPersonForEmail("user1@slac.stanford.edu")
        );
        // now superseed the original entry
        entryService.createNewSupersede(
                newLogID,
                EntryNewDTO
                        .builder()
                        .logbooks(List.of(logbook.id()))
                        .text("This is a superseed for log for log %s".formatted(newLogID))
                        .title("A very wonderful superseeded log")
                        .build(),
                sharedUtilityService.getPersonForEmail("user1@slac.stanford.edu")
        );

        // this now shouldn't gives error
        var result = assertDoesNotThrow(
                () -> entryService.findAll(
                        QueryWithAnchorDTO
                                .builder()
                                .limit(20)
                                .build()
                )
        );
        assertThat(result).hasSize(3);
    }

}
