package edu.stanford.slac.elog_plus.v1.service;

import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.migration.InitLogbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
public class LogbookDefaultInitTest {
    @Autowired
    LogbookService logbookService;

    @Test
    public void checkDefaultLogbookCreation() {
        List<LogbookDTO> allDefault = assertDoesNotThrow(
                () -> logbookService.getAllLogbook()
        );
        assertThat(allDefault).isNotNull();
        assertThat(allDefault.size()).isEqualTo(InitLogbook.logbookNames.size());
    }
}
