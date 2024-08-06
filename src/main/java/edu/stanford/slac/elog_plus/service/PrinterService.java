package edu.stanford.slac.elog_plus.service;

import com.hp.jipp.encoding.*;
import com.hp.jipp.model.*;
import com.hp.jipp.trans.IppPacketData;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static com.hp.jipp.encoding.Tag.*;

@Log4j2
@Service
@AllArgsConstructor
public class PrinterService {

    /**
     * Get the print command from the IPP packet
     *
     * @param inputData The IPP packet data
     * @return The print command
     */
    public int getPrintCommand(IppPacketData inputData){
        return inputData.getPacket().getOperation().getCode();
    }

    /**
     * Process an IPP packet
     *
     * @param inputData  The IPP packet data
     * @return The IPP packet data
     */
    public IppPacketData handleIppPacket(IppPacketData inputData) {
        IppPacket requestPacket = inputData.getPacket();
        log.info("Processing IPP request: " + requestPacket);

        IppPacket responsePacket;
        responsePacket = switch (getPrintCommand(inputData)) {
            case Operation.Code.printJob -> handlePrintDocument(requestPacket, inputData.getData());
            case Operation.Code.getPrinterAttributes -> handleGetPrinterAttributes(requestPacket);
            default -> createErrorResponsePacket(requestPacket, Status.clientErrorNotFetchable);
        };

        return new IppPacketData(responsePacket, null);
    }

    /**
     * Create an error response packet
     *
     * @param requestPacket         The request packet
     * @param status The status
     * @return The error response packet
     */
    public IppPacket createErrorResponsePacket(IppPacket requestPacket, Status status) {
        return IppPacket.response(status).build();
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

        // get the mimetype
        String mimeType = requestPacket.getString(operationAttributes, Types.documentFormat);
        if(mimeType==null) {
            // return error
            return IppPacket.jobResponse(
                    Status.clientErrorBadRequest,
                    requestPacket.getRequestId(),
                    URI.create("/v1/printers/defaults/jobs/1"),
                    JobState.aborted,
                    Collections.singletonList(JobStateReason.jobQueued))
                    .putAttributes(operationAttributes, Types.printerUri.of(URI.create("/v1/printers/defaults")))
                    .build();
        }
        try {

            if (documentStream != null) {
                if(mimeType.equalsIgnoreCase("text/plain")){
                    handleText(documentStream);
                } else {
                    handleImages(documentStream);
                }
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

    private void handleImages(InputStream documentStream) {
    }

    private void handleText(InputStream documentStream) throws IOException {
        StringBuilder documentContent = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(documentStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            documentContent.append(line).append("\n");
        }
        // we have all the document
    }

    /**
     * Handle a Get-Printer-Attributes request
     *
     * @param requestPacket The IPP request packet
     * @return The IPP response packet
     */
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
                                "image/gif",
                                "image/jpeg",
                                "image/pjpeg",
                                "image/png",
                                "image/svg+xml",
                                "image/tiff",
                                "text/plain"
                        )
                )
                .build();
    }
}
