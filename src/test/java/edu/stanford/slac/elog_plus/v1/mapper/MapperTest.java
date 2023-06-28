package edu.stanford.slac.elog_plus.v1.mapper;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogMapper;
import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static java.util.Arrays.asList;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class MapperTest {
    @Autowired
    AttachmentService attachmentService;
    @Test
    public void logDTOAuthorName() throws Exception {
        var log = Log.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = LogMapper.INSTANCE.fromModel(log, attachmentService);
        AssertionsForClassTypes.assertThat(logDto.author()).isEqualTo("firstName lastName");
    }

    @Test
    public void searchResultLogDTOAuthorName() throws Exception {
        var log = Log.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = LogMapper.INSTANCE.toSearchResultFromDTO(log, attachmentService);
        AssertionsForClassTypes.assertThat(logDto.author()).isEqualTo("firstName lastName");
    }

    @Test
    public void createModelFromNewLog() {
        Log newLog = LogMapper.INSTANCE.fromDTO(
                NewLogDTO.builder().build(),
                "firstName",
                "lastName",
                "userName");
        AssertionsForClassTypes.assertThat(newLog.getFirstName()).isEqualTo("firstName");
        AssertionsForClassTypes.assertThat(newLog.getLastName()).isEqualTo("lastName");
        AssertionsForClassTypes.assertThat(newLog.getUserName()).isEqualTo("userName");
    }

    @Test
    public void newLogDTOToModel() throws Exception {
        var newLog = NewLogDTO
                .builder()
                .attachments(List.of("att_1", "arr_2"))
                .title("title")
                .segment("segment")
                .text("text")
                .tags(List.of("tags1", "tags2"))
                .logbook("Logbook")
                .build();
        Faker faker = new Faker();
        var logModel = LogMapper.INSTANCE.fromDTO(newLog, faker.name().firstName(), faker.name().lastName(), faker.name().username());
        AssertionsForClassTypes.assertThat(logModel.getText()).isEqualTo("text");
        AssertionsForClassTypes.assertThat(logModel.getAttachments()).isEqualTo(List.of("att_1", "arr_2"));
        AssertionsForClassTypes.assertThat(logModel.getTitle()).isEqualTo("title");
        AssertionsForClassTypes.assertThat(logModel.getSegment()).isEqualTo("segment");
        AssertionsForClassTypes.assertThat(logModel.getTags()).isEqualTo(List.of("tags1", "tags2"));
        AssertionsForClassTypes.assertThat(logModel.getLogbook()).isEqualTo("Logbook");
        AssertionsForClassTypes.assertThat(logModel.getUserName()).isNotNull();
        AssertionsForClassTypes.assertThat(logModel.getFirstName()).isNotNull();
        AssertionsForClassTypes.assertThat(logModel.getLastName()).isNotNull();
    }
}
