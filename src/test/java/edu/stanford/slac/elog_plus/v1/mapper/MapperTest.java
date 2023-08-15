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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class MapperTest {
    @Autowired
    EntryMapper entryMapper;
    @Test
    public void logDTOAuthorName() throws Exception {
        var log = Entry.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = entryMapper.fromModel(log);
        AssertionsForClassTypes.assertThat(logDto.loggedBy()).isEqualTo("firstName lastName");
    }

    @Test
    public void searchResultLogDTOAuthorName() throws Exception {
        var log = Entry.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = entryMapper.toSearchResultFromDTO(log);
        AssertionsForClassTypes.assertThat(logDto.loggedBy()).isEqualTo("firstName lastName");
    }

    @Test
    public void createModelFromNewLog() {
        Entry newEntry = entryMapper.fromDTO(
                EntryNewDTO.builder().build(),
                "firstName",
                "lastName",
                "userName");
        AssertionsForClassTypes.assertThat(newEntry.getFirstName()).isEqualTo("firstName");
        AssertionsForClassTypes.assertThat(newEntry.getLastName()).isEqualTo("lastName");
        AssertionsForClassTypes.assertThat(newEntry.getUserName()).isEqualTo("userName");
    }
}
