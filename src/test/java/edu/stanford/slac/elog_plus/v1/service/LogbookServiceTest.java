package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.assertj.core.api.AssertionsForClassTypes;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        String newID = getTestLogbook();

        assertThat(newID).isNotNull().isNotEmpty();
    }

    @Test
    public void fetchAll() {
        String newID = getTestLogbook();

        assertThat(newID).isNotNull().isNotEmpty();

        List<LogbookDTO> allLogbook = assertDoesNotThrow(
                () -> logbookService.getAllLogbook()
        );

        assertThat(allLogbook).isNotNull().isNotEmpty();
    }

    @Test
    public void createTag() {
        String newLogbookID = getTestLogbook();
        AssertionsForClassTypes.assertThat(newLogbookID).isNotNull().isNotEmpty();

        String newTagID = assertDoesNotThrow(
                () -> logbookService.createNewTag(
                        newLogbookID,
                        NewTagDTO
                                .builder()
                                .name("new-tag")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newTagID).isNotNull().isNotEmpty();

        LogbookDTO fullLogbook = assertDoesNotThrow(
                () -> logbookService.getLogbook(newLogbookID)
        );
        assertThat(fullLogbook).isNotNull();
        assertThat(fullLogbook.tags()).isNotEmpty();

        List<TagDTO> allTags = assertDoesNotThrow(
                () -> logbookService.getAllTags(newLogbookID)
        );
        assertThat(allTags).isNotNull();
        assertThat(allTags).isNotEmpty();

        assertThat(fullLogbook.tags()).containsAll(allTags);
    }

    @Test
    public void failAddingShiftWithBadTImeFrom() {
        String newLogbookID = getTestLogbook();
        ControllerLogicException exceptBadTime = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("35:73")
                                .to("48:81")
                                .build()
                )
        );
        assertThat(exceptBadTime.getErrorCode()).isEqualTo(-1);
        assertThat(exceptBadTime.getErrorMessage()).containsPattern(".*'from'.*range 00:01-23:59");

        exceptBadTime = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:73")
                                .to("48:81")
                                .build()
                )
        );
        assertThat(exceptBadTime.getErrorCode()).isEqualTo(-1);
        assertThat(exceptBadTime.getErrorMessage()).containsPattern(".*'from'.*range 00:01-23:59");

        exceptBadTime = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:30")
                                .to("48:81")
                                .build()
                )
        );
        assertThat(exceptBadTime.getErrorCode()).isEqualTo(-1);
        assertThat(exceptBadTime.getErrorMessage()).containsPattern(".*'to'.*range 00:01-23:59");

        exceptBadTime = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:30")
                                .to("00:81")
                                .build()
                )
        );
        assertThat(exceptBadTime.getErrorCode()).isEqualTo(-1);
        assertThat(exceptBadTime.getErrorMessage()).containsPattern(".*'to'.*range 00:01-23:59");

        exceptBadTime = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:30")
                                .to("00:20")
                                .build()
                )
        );
        assertThat(exceptBadTime.getErrorCode()).isEqualTo(-1);
        assertThat(exceptBadTime.getErrorMessage()).containsPattern(".*'from'.*before.*'to'.*");
    }

    @Test
    public void shiftAddFailNoLogbook() {
        ControllerLogicException exceptNoLogbook = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        "wrong id",
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:30")
                                .to("00:50")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(exceptNoLogbook.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void shiftAddOk() {
        String newLogbookID = getTestLogbook();
        String shiftId = assertDoesNotThrow(
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:01")
                                .to("03:59")
                                .build()
                )
        );
        assertThat(shiftId).isNotNull().isNotEmpty();

        LogbookDTO fullLogbook = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookID
                )
        );

        assertThat(fullLogbook).isNotNull();
        assertThat(fullLogbook.shifts()).extracting("id").contains(shiftId);
    }

    @Test
    public void shiftAddFailOnOverlapping() {
        String newLogbookID = getTestLogbook();
        String shiftId1 = assertDoesNotThrow(
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:01")
                                .to("00:59")
                                .build()
                )
        );
        assertThat(shiftId1).isNotNull().isNotEmpty();

        String shiftId2 = assertDoesNotThrow(
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift2")
                                .from("02:01")
                                .to("02:59")
                                .build()
                )
        );
        assertThat(shiftId2).isNotNull().isNotEmpty();

        // fails on various overlapping rules

        ControllerLogicException exceptOverlap = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("ShiftFails")
                                .from("00:30")
                                .to("02:20")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(exceptOverlap.getErrorCode()).isEqualTo(-3);
        AssertionsForClassTypes.assertThat(exceptOverlap.getErrorMessage()).containsPattern(".*'from'.*overlap.*Shift1");

        exceptOverlap = assertThrows(
                ControllerLogicException.class,
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("ShiftFails")
                                .from("01:00")
                                .to("02:20")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(exceptOverlap.getErrorCode()).isEqualTo(-3);
        AssertionsForClassTypes.assertThat(exceptOverlap.getErrorMessage()).containsPattern(".*'to'.*overlap.*Shift2");
    }

    @Test
    public void shiftAddOkInTheMiddle() {
        String newLogbookID = getTestLogbook();
        String shiftId1 = assertDoesNotThrow(
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift1")
                                .from("00:00")
                                .to("00:59")
                                .build()
                )
        );
        assertThat(shiftId1).isNotNull().isNotEmpty();

        String shiftId2 = assertDoesNotThrow(
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift2")
                                .from("02:00")
                                .to("02:59")
                                .build()
                )
        );
        assertThat(shiftId2).isNotNull().isNotEmpty();

        String shiftId3 = assertDoesNotThrow(
                () -> logbookService.addShift(
                        newLogbookID,
                        NewShiftDTO
                                .builder()
                                .name("Shift3")
                                .from("01:00")
                                .to("01:59")
                                .build()
                )
        );
        assertThat(shiftId3).isNotNull().isNotEmpty();


        LogbookDTO fullLogbook = assertDoesNotThrow(
                () -> logbookService.getLogbook(
                        newLogbookID
                )
        );

        assertThat(fullLogbook).isNotNull();
        assertThat(fullLogbook.shifts()).extracting("id").contains(shiftId1, shiftId2, shiftId3);
    }

    private String getTestLogbook() {
        String newLogbookID = assertDoesNotThrow(
                () -> logbookService.createNew(
                        NewLogbookDTO
                                .builder()
                                .name("new-logbook")
                                .build()
                )
        );
        AssertionsForClassTypes.assertThat(newLogbookID).isNotNull().isNotEmpty();
        return newLogbookID;
    }
}
