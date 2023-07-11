package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.service.TagService;
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
public class TagServiceTest {
    @Autowired
    TagService tagService;
    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Tag.class);
    }

    @Test
    public void tagCreation() {
        ControllerLogicException exception =
                assertThrows(
                        ControllerLogicException.class,
                        ()->tagService.createTag(NewTagDTO.builder().build())
                );
        assertThat(exception.getErrorCode()).isEqualTo(-1);
    }

    @Test
    public void exceptionOnNotFoundID() {
        ControllerLogicException exception =
                assertThrows(
                        ControllerLogicException.class,
                        ()->tagService.getByID("bad-id")
                );
        assertThat(exception.getErrorCode()).isEqualTo(-2);
    }

    @Test
    public void createAndVerify(){
        String createdTagID =
                assertDoesNotThrow(
                        ()->tagService.createTag(
                                NewTagDTO
                                        .builder()
                                        .name("new-tag")
                                        .build()
                        )
                );

        TagDTO newTag = assertDoesNotThrow(
                ()->tagService.getByID(
                        createdTagID
                )
        );

        assertThat(newTag.name()).isEqualTo("new-tag");

        List<TagDTO> allTags = assertDoesNotThrow(
                ()->tagService.getAllTags()
        );
        assertThat(allTags.size()).isEqualTo(1);
    }

    @Test
    public void normalizeTagName() {
        final String input = "Tĥïŝ ĩš â fůňķŷ Šťŕĭńġ";
        String output = tagService.tagNameNormalization(input);
        assertThat(output).isEqualTo("This is a funky String");
    }

    @Test
    public void normalizeTagNameLowerCase() {
        final String input = "This Is With Upper Case";
        String output = tagService.tagNameNormalization(input);
        assertThat(output).isEqualTo("this is with upper case");
    }

    @Test
    public void normalizeTagNameWithSpace() {
        final String input = " with space ";
        String output = tagService.tagNameNormalization(input);
        assertThat(output).isEqualTo("with_space");
    }
}
