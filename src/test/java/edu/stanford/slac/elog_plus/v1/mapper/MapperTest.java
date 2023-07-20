package edu.stanford.slac.elog_plus.v1.mapper;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryNewDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper;
import edu.stanford.slac.elog_plus.model.Entry;
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
        var log = Entry.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = EntryMapper.INSTANCE.fromModel(log, attachmentService);
        AssertionsForClassTypes.assertThat(logDto.loggedBy()).isEqualTo("firstName lastName");
    }

    @Test
    public void searchResultLogDTOAuthorName() throws Exception {
        var log = Entry.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = EntryMapper.INSTANCE.toSearchResultFromDTO(log, attachmentService);
        AssertionsForClassTypes.assertThat(logDto.loggedBy()).isEqualTo("firstName lastName");
    }

    @Test
    public void createModelFromNewLog() {
        Entry newEntry = EntryMapper.INSTANCE.fromDTO(
                EntryNewDTO.builder().build(),
                "firstName",
                "lastName",
                "userName");
        AssertionsForClassTypes.assertThat(newEntry.getFirstName()).isEqualTo("firstName");
        AssertionsForClassTypes.assertThat(newEntry.getLastName()).isEqualTo("lastName");
        AssertionsForClassTypes.assertThat(newEntry.getUserName()).isEqualTo("userName");
    }

    @Test
    public void newLogDTOToModel() throws Exception {
        var newLog = EntryNewDTO
                .builder()
                .attachments(List.of("att_1", "arr_2"))
                .title("title")
                .segment("segment")
                .text("text")
                .tags(List.of("tags1", "tags2"))
                .logbook("Logbook")
                .build();
        Faker faker = new Faker();
        var logModel = EntryMapper.INSTANCE.fromDTO(newLog, faker.name().firstName(), faker.name().lastName(), faker.name().username());
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
