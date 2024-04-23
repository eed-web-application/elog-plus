package edu.stanford.slac.elog_plus.v1.consumer;

import com.github.javafaker.Faker;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.AuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v2.dto.ImportEntryDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.EntryService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.v1.controller.TestControllerHelperService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@PropertySource("classpath:application.yml")
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ProcessLogImportTest {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private AuthService authService;
    @Autowired
    private JWTHelper jwtHelper;
    @Autowired
    private LogbookService logbookService;
    @Autowired
    private EntryService entryService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Autowired
    private KafkaTemplate<String, ImportEntryDTO> importEntryDTOKafkaTemplate;
    @Value("${edu.stanford.slac.elog-plus.import-entry-topic}")
    private String importEntryTopic;
    @Value("${edu.stanford.slac.elog-plus.image-preview-topic}")
    private String imagePreviewTopic;
    @Autowired
    private KafkaAdmin kafkaAdmin;

    @BeforeEach
    public void resetData() {
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Attachment.class);
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Authorization.class);
        mongoTemplate.remove(new Query(), AuthenticationToken.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            List<String> topicsToDelete = List.of(
                    importEntryTopic,
                    String.format("%s-retry-2000", importEntryTopic),
                    String.format("%s-retry-4000", importEntryTopic),
                    imagePreviewTopic,
                    String.format("%s-retry-2000", imagePreviewTopic),
                    String.format("%s-retry-4000", imagePreviewTopic)
            );

            // Delete topics that actually exist
            topicsToDelete.stream()
                    .filter(existingTopics::contains)
                    .forEach(topic -> {
                        try {
                            adminClient.deleteTopics(Collections.singletonList(topic)).all().get();
                        } catch (Exception e) {
                            System.err.println("Failed to delete topic " + topic + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to recreate Kafka topic", e);
        }
    }

    @Test
    public void testSubmitImport() {
        Faker faker = new Faker();
        ImportEntryDTO dto = ImportEntryDTO
                .builder()
                // authorized user 2 to read on created logbook
                .readerUserIds(List.of("user2@slac.stanford.edu"))
                .entry(
                        EntryImportDTO
                                .builder()
                                .logbooks(List.of("new-logbook"))
                                .title(faker.book().title())
                                .text(faker.lorem().paragraph())
                                .originId("my-original-id")
                                .build()
                )
                .build();
        ProducerRecord<String, ImportEntryDTO> message = new ProducerRecord<>(importEntryTopic, "key", dto);
        message.headers().add("Authorization", jwtHelper.generateJwt("user1@slac.stanford.edu").getBytes());

        var sendData = importEntryDTOKafkaTemplate.send(message);
        var result = assertDoesNotThrow(() -> sendData.get());

        await()
                .atMost(30, SECONDS)
                .pollDelay(2, SECONDS)
                .until(() -> logbookService.existByName("new-logbook"));

        await()
                .atMost(30, SECONDS)
                .pollDelay(2, SECONDS)
                .until(() -> {
                    var foundEntries = assertDoesNotThrow(
                            ()->testControllerHelperService.submitSearchByGetWithAnchor(
                                    mockMvc,
                                    status().isOk(),
                                    Optional.of("user2@slac.stanford.edu"),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.of(10),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.empty(),
                                    Optional.of("my-original-id")
                            )
                    );
                    return foundEntries.getErrorCode() == 0 && !foundEntries.getPayload().isEmpty();
                });

        var foundEntries = assertDoesNotThrow(
                ()->testControllerHelperService.submitSearchByGetWithAnchor(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user2@slac.stanford.edu"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(10),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of("my-original-id")
                )
        );
        assertThat(foundEntries.getErrorCode()).isEqualTo(0);
        assertThat(foundEntries.getPayload().getFirst().title()).isEqualTo(dto.entry().title());
    }
}
