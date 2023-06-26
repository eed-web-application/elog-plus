package edu.stanford.slac.elog_plus;

import edu.stanford.slac.elog_plus.api.v1.mapper.LogMapper;
import edu.stanford.slac.elog_plus.model.Log;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
public class DTOTest {
    @Test
    public void logDTOAuthorName() throws Exception {
        var log = Log.builder()
                .firstName("firstName")
                .lastName("lastName")
                .build();
        var logDto = LogMapper.INSTANCE.fromModel(log);
        AssertionsForClassTypes.assertThat(logDto.author()).isEqualTo("firstName lastName");
//        AssertionsForClassTypes.assertThat(queryParameter).isNotNull();
//        AssertionsForClassTypes.assertThat(queryParameter.getErrorCode()).isEqualTo(0);
//        AssertionsForClassTypes.assertThat(queryParameter.getPayload()).isNotNull();
//        AssertionsForClassTypes.assertThat(queryParameter.getPayload().logbook().size())
//                .isEqualTo(
//                        queryParameterConfigurationDTO.logbook().size()
//                );
    }
}
