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
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.v1.service.DocumentGenerationService;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import static com.hp.jipp.encoding.Tag.nameWithoutLanguage;
import static com.hp.jipp.encoding.Tag.printerAttributes;
import static com.hp.jipp.model.Types.*;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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

    private LogbookDTO fullLogbook;

    @BeforeEach
    public void preTest() {
        mongoTemplate.remove(new Query(), Logbook.class);
        mongoTemplate.remove(new Query(), Entry.class);
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
            response = assertDoesNotThrow(() -> sendRequest(Optional.of("user1@slac.stanford.edu"), request, status().isOk()));
            assertThat(response).isNotNull();
            System.out.println("\nReceived: " + response.getPacket().prettyPrint(100, "  "));
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
                System.out.println("\nReceived: " + responsePacket.getPacket().prettyPrint(100, "  "));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testPrintingImage() {
        // check if printer support png
        List<String> formats;
        try (var formatResponsePacket = assertDoesNotThrow(() -> getPrinterProperties(Optional.of("user1@slac.stanford.edu"), status().isOk()))) {
            // Make sure the format is supported
            System.out.println("\nReceived: " + formatResponsePacket.getPacket().prettyPrint(100, "  "));
            formats = formatResponsePacket.getPacket().getStrings(printerAttributes, documentFormatSupported);
            assertThat(formats.contains(MediaType.IMAGE_PNG_VALUE)).isTrue();
        }

        try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPng())) {
            try (var responsePacket = assertDoesNotThrow(() -> print(Optional.of("user1@slac.stanford.edu"), is, fullLogbook.name(), status().isOk()))) {
                assertThat(responsePacket).isNotNull();
                System.out.println("\nReceived: " + responsePacket.getPacket().prettyPrint(100, "  "));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void testPrintingText() {
        // check if printer support png
        List<String> formats;
        try (var formatResponsePacket = assertDoesNotThrow(() -> getPrinterProperties(Optional.of("user1@slac.stanford.edu"), status().isOk()))) {
            // Make sure the format is supported
            System.out.println("\nReceived: " + formatResponsePacket.getPacket().prettyPrint(100, "  "));
            formats = formatResponsePacket.getPacket().getStrings(printerAttributes, documentFormatSupported);
            assertThat(formats.contains(MediaType.IMAGE_PNG_VALUE)).isTrue();
        }

        try (InputStream is = assertDoesNotThrow(() -> documentGenerationService.getTestPng())) {
            try (var responsePacket = assertDoesNotThrow(() -> print(Optional.of("user1@slac.stanford.edu"), new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)), fullLogbook.name(), status().isOk()))) {
                assertThat(responsePacket).isNotNull();
                System.out.println("\nReceived: " + responsePacket.getPacket().prettyPrint(100, "  "));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public IppPacketData getJobStatus(Optional<String> userInfo, ResultMatcher status, int jobId) throws Exception {
        // Query for supported document formats
        IppPacket attributeRequest = IppPacket.getJobAttributes(URI.create("/v1/printers/defaults"))
                .putOperationAttributes(
                        requestingUserName.of(userInfo.get()),
                        Types.jobId.of(jobId)
                )
                .build();
        IppPacketData request = new IppPacketData(attributeRequest);
        return sendRequest(userInfo, request, status);
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
        return sendRequest(userInfo, request, status);
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
        IppPacket.Builder printRequestBuilder = IppPacket.printJob(URI.create("/v1/printers/defaults"));
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
        return sendRequest(userInfo, request, status);
    }

    /**
     * Send an IPP request and return the response
     *
     * @param request       The IPP request
     * @param resultMatcher The expected result
     * @return The IPP response
     * @throws Exception If an error occurs
     */
    private IppPacketData sendRequest(Optional<String> userInfo, IppPacketData request, ResultMatcher resultMatcher) throws Exception {
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

        var postBuilder = post("/v1/printers/defaults")
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
