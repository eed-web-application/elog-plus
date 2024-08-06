package edu.stanford.slac.elog_plus.api.v1.controller;

import com.hp.jipp.encoding.*;
import com.hp.jipp.model.*;
import com.hp.jipp.trans.IppPacketData;
import com.hp.jipp.util.PrettyPrinter;
import edu.stanford.slac.elog_plus.api.v1.dto.PrintJobDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.common.metrics.Stat;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.hp.jipp.encoding.Tag.operationAttributes;
import static com.hp.jipp.encoding.Tag.printerAttributes;

@Log4j2
@Validated
@RestController()
@RequestMapping("/v1/printers")
@AllArgsConstructor
@Schema(description = "Api for authentication information")
public class PrinterController {
    List<PrintJobDTO> printQueue = new ArrayList<>();

    @PostMapping("/defaults")
    public ResponseEntity<byte[]> defaultPrinter(
            HttpServletRequest request,
            @RequestParam("jwt") Optional<String> jwtToken,
            @RequestHeader Map<String, String> headers,
            @RequestBody byte[] body
    ) throws IOException {
        try {
            IppInputStream inputStream = new IppInputStream(new ByteArrayInputStream(body));
            IppPacketData data = new IppPacketData(inputStream.readPacket(), inputStream);
            IppPacketData response = handleIppPacket(request.getRequestURI(), data);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IppOutputStream ippOutputStream = new IppOutputStream(outputStream);
            ippOutputStream.write(response.getPacket());

            InputStream extraData = response.getData();
            if (extraData != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = extraData.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                extraData.close();
            }

            return ResponseEntity.ok()
                    .header("Content-Type", "application/ipp")
                    .body(outputStream.toByteArray());

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private IppPacketData handleIppPacket(String requestUri, IppPacketData data) {
        IppPacket requestPacket = data.getPacket();
        log.info("Processing IPP request: " + requestPacket);

        IppPacket responsePacket;
        int operationId = requestPacket.getOperation().getCode();

        responsePacket = switch (operationId) {
            case Operation.Code.printJob -> handlePrintDocument(requestPacket, data.getData());
            case Operation.Code.getPrinterAttributes -> handleGetPrinterAttributes(requestPacket);
            default -> createErrorResponsePacket(requestPacket, Status.clientErrorNotFetchable);
        };

        return new IppPacketData(responsePacket, null);
    }

    private IppPacket createErrorResponsePacket(IppPacket requestPacket, Status clientErrorBadRequest) {
        return IppPacket.response(clientErrorBadRequest).build();
    }

    /**
     * Handle a Print-Job request
     *
     * @param requestPacket  The IPP request packet
     * @param documentStream The document stream
     * @return The IPP response packet
     */
    private IppPacket handlePrintDocument(IppPacket requestPacket, InputStream documentStream) {
        log.info("Received Print-Job request");
        // Fetch the document from the IPP packet
        IppPacket responsePacket = null;
        try {
            if (documentStream != null) {
                StringBuilder documentContent = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(documentStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    documentContent.append(line).append("\n");
                }
                log.info("Document content: " + documentContent.toString());
            }
            responsePacket = IppPacket.jobResponse(
                            Status.successfulOk,
                            requestPacket.getRequestId(),
                            URI.create("/v1/printers/defaults/jobs/1"),
                            JobState.pending,
                            Collections.singletonList(JobStateReason.jobQueued))
                    .putAttributes(operationAttributes, Types.printerUri.of(URI.create("/v1/printers/defaults")))
                    .build();
        } catch (IOException e) {
            responsePacket = new IppPacket(Status.clientErrorBadRequest, requestPacket.getRequestId());
        }
        // Return the response packet
        return responsePacket;
    }

    private IppPacket handleGetPrinterAttributes(IppPacket requestPacket) {
        return IppPacket
                .response(Status.successfulOk)
                .putAttributes(
                        operationAttributes,
                        Types.printerUri.of(URI.create("/v1/printers/defaults")),
                        Types.attributesCharset.of("utf-8"),
                        Types.attributesNaturalLanguage.of("en")
                )
                .putAttributes(
                        printerAttributes,
                        Types.printerName.of("ElogIPPPrinterInterface"),
                        Types.printerState.of(PrinterState.idle),
                        Types.printerStateReasons.of("none"),
                        Types.operationsSupported.of(
                                Operation.printJob,
                                Operation.getJobAttributes,
                                Operation.getPrinterAttributes
                        ),
                        Types.documentFormatSupported.of(
                                "application/pdf",
                                "application/postscript",
                                "application/vnd.cups-raster",
                                "application/text"
                        )
                )
                .build();
    }
}
