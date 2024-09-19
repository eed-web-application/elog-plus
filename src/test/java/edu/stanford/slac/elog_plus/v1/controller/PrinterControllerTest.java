package edu.stanford.slac.elog_plus.v1.controller;

import com.hp.jipp.encoding.IppInputStream;
import com.hp.jipp.encoding.IppOutputStream;
import com.hp.jipp.encoding.IppPacket;
import com.hp.jipp.encoding.NameType;
import com.hp.jipp.model.Status;
import com.hp.jipp.model.Types;
import com.hp.jipp.trans.IppPacketData;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.auth.JWTHelper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.v1.service.DocumentGenerationService;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.OllamaResult;
import io.github.ollama4j.types.OllamaModelType;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;
import org.apache.kafka.clients.admin.AdminClient;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.MimeTypeUtils;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.hp.jipp.encoding.Tag.printerAttributes;
import static com.hp.jipp.model.Types.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest()
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PrinterControllerTest {
    @Autowired
    private JWTHelper jwtHelper;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppProperties appProperties;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private AuthService authService;
    @Autowired
    private DocumentGenerationService documentGenerationService;
    @Autowired
    private TestControllerHelperService testControllerHelperService;
    @Value("${edu.stanford.slac.elog-plus.image-preview-topic}")
    private String imagePreviewTopic;
    @Autowired
    private KafkaAdmin kafkaAdmin;
    private LogbookDTO fullLogbook;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Entry.class);
        mongoTemplate.remove(new Query(), Attachment.class);
        //reset authorizations
        mongoTemplate.remove(new Query(), Authorization.class);
        appProperties.getRootUserList().clear();
        appProperties.getRootUserList().add("user1@slac.stanford.edu");
        authService.updateRootUser();

        ApiResultResponse<String> testLogbookIdResult = assertDoesNotThrow(
                () -> testControllerHelperService.createNewLogbook(
                        mockMvc,
                        status().isCreated(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        NewLogbookDTO.builder()
                                .name("new-logbooks")
                                .build()
                )
        );
        assertThat(testLogbookIdResult).isNotNull();
        assertThat(testLogbookIdResult.getPayload()).isNotNull();
        var fullLogbookResult = assertDoesNotThrow(
                () -> testControllerHelperService.getLogbookByID(
                        mockMvc,
                        status().isOk(),
                        Optional.of(
                                "user1@slac.stanford.edu"
                        ),
                        testLogbookIdResult.getPayload()
                )
        );
        assertThat(fullLogbookResult).isNotNull();
        assertThat(fullLogbookResult.getPayload()).isNotNull();
        fullLogbook = fullLogbookResult.getPayload();

        //remove attachment on kafka queue
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            List<String> topicsToDelete = List.of(
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
    public void testPrinterAttribute() {
        var uri = URI.create("/v1/printers/defaults");
        IppPacket attributeRequestResponse = IppPacket.getPrinterAttributes(uri)
                .putOperationAttributes
                        (
                                requestingUserName.of("user1@slac.stanford.edu"),
                                requestedAttributes.of("all")
                        )
                .build();
        IppPacketData response = null;
        try (IppPacketData request = new IppPacketData(attributeRequestResponse)) {
            response = assertDoesNotThrow(() -> sendRequest("/v1/printers/default", Optional.of("user1@slac.stanford.edu"), request, status().isOk()));
            assertThat(response).isNotNull();
            assertThat(response.getPacket().getStatus()).isEqualTo(Status.successfulOk);
        } finally {
            if (response != null) response.close();
        }

    }

    @Test
    public void failPrintingWithNoAuth() {
        // check if printer support png
        try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPng())) {
            try (var responsePacket = assertDoesNotThrow(() -> print(Optional.empty(), is, MediaType.IMAGE_PNG_VALUE, status().isOk()))) {
                assertThat(responsePacket).isNotNull();
                assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.clientErrorNotAuthorized);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPrintingPNGImage() {
        // check if printer support png
        List<String> formats;
        try (var formatResponsePacket = assertDoesNotThrow(() -> getPrinterProperties(Optional.of("user1@slac.stanford.edu"), status().isOk()))) {
            // Make sure the format is supported
            assertThat(formatResponsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
            System.out.println("\nReceived: " + formatResponsePacket.getPacket().prettyPrint(100, "  "));
            formats = formatResponsePacket.getPacket().getStrings(printerAttributes, documentFormatSupported);
            assertThat(formats.contains(MediaType.IMAGE_PNG_VALUE)).isTrue();
        }

        try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPng())) {
            try (var responsePacket = assertDoesNotThrow(() -> print(Optional.of("user1@slac.stanford.edu"), is, fullLogbook.name(), status().isOk()))) {
                assertThat(responsePacket).isNotNull();
                assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    @Test
    public void testPrintingPSImage() {
        // check if printer support png
        List<String> formats;
        try (var formatResponsePacket = assertDoesNotThrow(() -> getPrinterProperties(Optional.of("user1@slac.stanford.edu"), status().isOk()))) {
            // Make sure the format is supported
            assertThat(formatResponsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
            System.out.println("\nReceived: " + formatResponsePacket.getPacket().prettyPrint(100, "  "));
            formats = formatResponsePacket.getPacket().getStrings(printerAttributes, documentFormatSupported);
            assertThat(formats.contains("application/postscript")).isTrue();
        }

        try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPS())) {
            try (var responsePacket = assertDoesNotThrow(() -> print(Optional.of("user1@slac.stanford.edu"), is, fullLogbook.name(), status().isOk()))) {
                assertThat(responsePacket).isNotNull();
                assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPrintingPDFImage() {
        // check if printer support png
        List<String> formats;
        try (var formatResponsePacket = assertDoesNotThrow(() -> getPrinterProperties(Optional.of("user1@slac.stanford.edu"), status().isOk()))) {
            // Make sure the format is supported
            System.out.println("\nReceived: " + formatResponsePacket.getPacket().prettyPrint(100, "  "));
            formats = formatResponsePacket.getPacket().getStrings(printerAttributes, documentFormatSupported);
            assertThat(formats.contains(MediaType.APPLICATION_PDF_VALUE)).isTrue();
        }

        assertDoesNotThrow(() -> {
                    try (var pdfDocument = documentGenerationService.generatePdf()) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        pdfDocument.save(outputStream);
                        try (InputStream is = new ByteArrayInputStream(outputStream.toByteArray())) {
                            try (var responsePacket = assertDoesNotThrow(() -> print(Optional.of("user1@slac.stanford.edu"), is, fullLogbook.name(), status().isOk()))) {
                                assertThat(responsePacket).isNotNull();
                                assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
                            }
                        }
                    }
                }
        );
    }

    @Test
    public void testPrintingText() {
        try (var responsePacket = assertDoesNotThrow(() -> print(Optional.of("user1@slac.stanford.edu"), new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)), fullLogbook.name(), status().isOk()))) {
            assertThat(responsePacket).isNotNull();
            System.out.println("\nReceived: " + responsePacket.getPacket().prettyPrint(100, "  "));
        }
    }

    @Test
    public void testAttachmentQueue() {
        // check if printer support png
        int numberOfDocument = 10;

        // creates some attachments to be sure that those are not included on the attachment queue
        for(int idx =0; idx < numberOfDocument ; idx++) {
            try (InputStream is = assertDoesNotThrow(
                    () -> documentGenerationService.getTestPng()
            )) {
                ApiResultResponse<String> newAttachmentID = assertDoesNotThrow(() -> testControllerHelperService.newAttachment(
                                mockMvc,
                                status().isCreated(),
                                Optional.of(
                                        "user1@slac.stanford.edu"
                                ),
                                new MockMultipartFile(
                                        "uploadFile",
                                        "image.pdf",
                                        MediaType.IMAGE_PNG_VALUE,
                                        is.readAllBytes()
                                )
                        )
                );
                assertThat(newAttachmentID).isNotNull();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        //randomly generate a list of documents from pdf , jpg, png to be queued using printer
        for (int i = 0; i < numberOfDocument; i++) {
            var typeOfDocument = (int) (Math.random() * 3);
            switch (typeOfDocument) {
                // png
                case 0 -> assertDoesNotThrow(() -> {
                    try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPng())) {
                        try (var responsePacket = assertDoesNotThrow(() -> printOnAttachmentQueue(Optional.of("user1@slac.stanford.edu"), is, status().isOk()))) {
                            assertThat(responsePacket).isNotNull();
                            assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                // postscript
                case 1 -> assertDoesNotThrow(() -> {
                    try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPS())) {
                        try (var responsePacket = assertDoesNotThrow(() -> printOnAttachmentQueue(Optional.of("user1@slac.stanford.edu"), is, status().isOk()))) {
                            assertThat(responsePacket).isNotNull();
                            assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                // pdf
                case 2 -> assertDoesNotThrow(() -> {
                    try (var pdfDocument = documentGenerationService.generatePdf()) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        assertDoesNotThrow(() -> pdfDocument.save(outputStream));
                        try (InputStream is = new ByteArrayInputStream(outputStream.toByteArray())) {
                            try (var responsePacket = assertDoesNotThrow(() -> printOnAttachmentQueue(Optional.of("user1@slac.stanford.edu"), is, status().isOk()))) {
                                assertThat(responsePacket).isNotNull();
                                assertThat(responsePacket.getPacket().getStatus()).isEqualTo(Status.successfulOk);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                default -> throw new IllegalStateException("Unexpected value: " + typeOfDocument);
            }
        }

        // fetch all queued attachment waiting to be processed
        var attachmentList = assertDoesNotThrow(() -> testControllerHelperService.attachmentControllerFindAllQueued(
                        mockMvc,
                        status().isOk(),
                        Optional.of("user1@slac.stanford.edu")
                )
        );
        assertThat(attachmentList).isNotNull();
        assertThat(attachmentList.getPayload()).isNotNull();
        assertThat(attachmentList.getPayload().size()).isEqualTo(numberOfDocument);

        // wait for preview generation
        await()
                .atMost(60, SECONDS)
                .pollInterval(1, SECONDS)
                .until(
                        () -> {
                            var attachmentListForCheck = assertDoesNotThrow(() -> testControllerHelperService.attachmentControllerFindAllQueued(
                                            mockMvc,
                                            status().isOk(),
                                            Optional.of("user1@slac.stanford.edu")
                                    )
                            );
                            // check that every attachment is completed
                            assertThat(attachmentListForCheck).isNotNull();
                            assertThat(attachmentListForCheck.getPayload()).isNotNull();
                            return attachmentListForCheck.getPayload().stream().allMatch(attachmentDTO -> attachmentDTO.previewState().compareTo(Attachment.PreviewProcessingState.Completed.name()) == 0);
                        }
                );
    }

    /**
     * Get the printer properties
     *
     * @param userInfo The user info
     * @param status   The expected status
     * @return The IPP packet data
     * @throws Exception If an error occurs
     */
    public IppPacketData getPrinterProperties(Optional<String> userInfo, ResultMatcher status) throws Exception {
        // Query for supported document formats
        IppPacket attributeRequest = IppPacket.getPrinterAttributes(URI.create("/v1/printers/defaults"))
                .putOperationAttributes(
                        requestingUserName.of(userInfo.get()),
                        requestedAttributes.of(documentFormatSupported.getName()))
                .build();
        IppPacketData request = new IppPacketData(attributeRequest);
        return sendRequest("/v1/printers/default", userInfo, request, status);
    }

    /**
     * Print a document
     *
     * @param userInfo The user info
     * @param is       The input stream
     * @param logbook  The logbook where to print
     * @param status   The expected status
     * @return The IPP packet data
     * @throws Throwable If an error occurs
     */
    public IppPacketData print(Optional<String> userInfo, InputStream is, String logbook, ResultMatcher status) throws Throwable {
        // Deliver the print request
        IppPacket.Builder printRequestBuilder = IppPacket.printJob(URI.create("/v1/printers/default"));
        printRequestBuilder.putOperationAttributes(
                requestingUserName.of("user"),
                documentFormat.of("application/octet-stream"));
        printRequestBuilder.putJobAttributes(new NameType.Set("logbook").of(logbook));
        userInfo.ifPresent(
                s -> printRequestBuilder.putJobAttributes(new NameType.Set("jwt").of(jwtHelper.generateJwt(s)))
        );
        IppPacket printRequest = printRequestBuilder.build();
        System.out.println("\nSending " + printRequest.prettyPrint(100, "  "));
        var request = new IppPacketData(printRequest, is);
        return sendRequest("/v1/printers/default", userInfo, request, status);
    }


    /**
     * Print a document on the attachment queue
     *
     * @param userInfo The user info
     * @param is       The input stream
     * @param status   The expected status
     * @return The IPP packet data
     * @throws Throwable If an error occurs
     */
    public IppPacketData printOnAttachmentQueue(Optional<String> userInfo, InputStream is, ResultMatcher status) throws Throwable {
        // Deliver the print request
        IppPacket.Builder printRequestBuilder = IppPacket.printJob(URI.create("/v1/printers/attachment-queue"));
        printRequestBuilder.putOperationAttributes(
                requestingUserName.of("user"),
                documentFormat.of("application/octet-stream"));
        userInfo.ifPresent(
                s -> printRequestBuilder.putJobAttributes(new NameType.Set("jwt").of(jwtHelper.generateJwt(s)))
        );
        IppPacket printRequest = printRequestBuilder.build();
        System.out.println("\nSending " + printRequest.prettyPrint(100, "  "));
        var request = new IppPacketData(printRequest, is);
        return sendRequest("/v1/printers/attachment-queue", userInfo, request, status);
    }

    /**
     * Send an IPP request and return the response
     *
     * @param request       The IPP request
     * @param resultMatcher The expected result
     * @return The IPP response
     * @throws Exception If an error occurs
     */
    private IppPacketData sendRequest(String httpURI, Optional<String> userInfo, IppPacketData request, ResultMatcher resultMatcher) throws Exception {
        // Copy IppPacket to the output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (IppOutputStream output = new IppOutputStream(outputStream)) {
            output.write(request.getPacket());
            InputStream extraData = request.getData();
            if (extraData != null) {
                copy(extraData, output);
                extraData.close();
            }
        }

        var postBuilder = post(httpURI)
                .contentType("application/ipp")
                .content(outputStream.toByteArray());
//        userInfo.ifPresent(login -> postBuilder.header(appProperties.getUserHeaderName(), jwtHelper.generateJwt(login)));
        MvcResult result = mockMvc.perform(postBuilder)
                .andExpect(resultMatcher)
                .andReturn();
        // Parse it back into an IPP packet
        IppInputStream responseInput = new IppInputStream(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        return new IppPacketData(responseInput.readPacket(), responseInput);
    }

    private void copy(InputStream data, OutputStream output) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int readAmount = data.read(buffer);
        while (readAmount != -1) {
            output.write(buffer, 0, readAmount);
            readAmount = data.read(buffer);
        }
    }
}
