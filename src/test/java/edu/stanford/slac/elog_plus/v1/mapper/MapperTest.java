package edu.stanford.slac.elog_plus.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.EntryNewDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper;
import edu.stanford.slac.elog_plus.model.Entry;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper.ELOG_ENTRY_REF;
import static edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper.ELOG_ENTRY_REF_ID;
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
        assertThat(logDto.loggedBy()).isEqualTo("firstName lastName");
    }

    @Test
    public void searchResultLogDTOAuthorName() throws Exception {
        var log = Entry.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = entryMapper.toSearchResult(log);
        assertThat(logDto.loggedBy()).isEqualTo("firstName lastName");
    }

    @Test
    public void createModelFromNewLog() {
        Entry newEntry = entryMapper.fromDTO(
                EntryNewDTO.builder().build(),
                "firstName",
                "lastName",
                "userName");
        assertThat(newEntry.getFirstName()).isEqualTo("firstName");
        assertThat(newEntry.getLastName()).isEqualTo("lastName");
        assertThat(newEntry.getUserName()).isEqualTo("userName");
    }

    @Test
    public void findReferenceInText() {
        Element fragment = new Element("div");
        fragment.appendText("This is a text with reference");
        fragment.appendElement(ELOG_ENTRY_REF).attr(ELOG_ENTRY_REF_ID, "uuid-reference1");
        fragment.appendElement(ELOG_ENTRY_REF).attr(ELOG_ENTRY_REF_ID, "uuid-reference2");
        Entry newEntry = entryMapper.fromDTO(
                EntryNewDTO
                        .builder()
                        .text(fragment.html())
                        .build(),
                "firstName",
                "lastName",
                "userName");
        assertThat(newEntry.getReferences()).contains("uuid-reference1","uuid-reference2");
    }
}
