package edu.stanford.slac.elog_plus.service;

import com.hp.jipp.encoding.IppPacket;
import com.hp.jipp.encoding.NameType;
import com.hp.jipp.model.*;
import com.hp.jipp.trans.IppPacketData;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.service.PeopleGroupService;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryNewDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final PeopleGroupService peopleGroupService;
    private final AppProperties appProperties;
    private final EntryService entryService;

    /**
     * Get the print command from the IPP packet
     *
     * @param inputData The IPP packet data
     * @return The print command
     */
    public int getPrintCommand(IppPacketData inputData) {
        return inputData.getPacket().getOperation().getCode();
    }

    /**
     * Get the logbook destination from the IPP packet
     *
     * @param inputData The IPP packet data
     * @return The logbook destination
     */
    public String getLogbookDestination(IppPacketData inputData) {
        return inputData.getPacket().getString(jobAttributes, new NameType.Set("logbook"));
    }

    public String getFileName(IppPacket packet) {
        return packet.getString(jobAttributes, new NameType.Set("document-name-supplied"));

    }

    /**
     * Process an IPP packet
     *
     * @param inputData The IPP packet data
     * @return The IPP packet data
     */
    public IppPacketData handleIppPacket(IppPacketData inputData, LogbookDTO logbook) {
        IppPacket requestPacket = inputData.getPacket();
        log.info("Processing IPP request: " + requestPacket);

        IppPacket responsePacket;
        responsePacket = switch (getPrintCommand(inputData)) {
            case Operation.Code.printJob -> handlePrintDocument(requestPacket, inputData.getData(), logbook);
            case Operation.Code.getPrinterAttributes -> handleGetPrinterAttributes(requestPacket);
            default -> createErrorResponsePacket(requestPacket, Status.clientErrorNotFetchable);
        };

        return new IppPacketData(responsePacket, null);
    }

    /**
     * Create an error response packet
     *
     * @param requestPacket The request packet
     * @param status        The status
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
    private IppPacket handlePrintDocument(IppPacket requestPacket, InputStream documentStream, LogbookDTO logbook) {
        log.info("Received Print-Job request for logbook {}", logbook.name());
        // Fetch the document from the IPP packet
        IppPacket responsePacket = null;
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        Metadata metadata = new Metadata();
        try {
            if (documentStream == null) {
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
            MediaType mediaType = detector.detect(documentStream, metadata);
            if (mediaType == null) {
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

            switch (mediaType.getBaseType().getType()) {
                case "text" -> handleTextBaseType(requestPacket, logbook, mediaType.getSubtype(), documentStream);
                case "image" -> handleImageBaseType(requestPacket, logbook, mediaType.getSubtype(), documentStream);
                case "application" -> handleApplicationBaseType(requestPacket, logbook, mediaType.getSubtype(), documentStream);
                default -> {return new IppPacket(Status.clientErrorBadRequest, requestPacket.getRequestId());}
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

    private void handleApplicationBaseType(IppPacket requestPacket, LogbookDTO logbookDTO, String subtype, InputStream documentStream) {
        log.info("Create entry form application print request for subtype: %s".formatted(subtype));
        if(subtype.equals("pdf")) {
            log.info("Create entry form pdf print request");
        }
    }

    private void handleImageBaseType(IppPacket requestPacket, LogbookDTO logbookDTO, String subtype, InputStream documentStream) {
        log.info("Create entry form image print request for subtype: %s".formatted(subtype));
    }

    private void handleTextBaseType(IppPacket requestPacket, LogbookDTO logbookDTO, String subtype, InputStream documentStream) throws IOException {
        log.info("Create entry form text document print request for subtype: %s".formatted(subtype));
        StringBuilder documentContent = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(documentStream, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            documentContent.append(line).append("\n");
        }
        String documentFileName = getFileName(requestPacket);
        PersonDTO creator = null;
        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();
        // we have all the document
        if (auth.getCredentials().toString().endsWith(appProperties.getAuthenticationTokenDomain())) {
            // create fake person for authentication token
            creator = PersonDTO
                    .builder()
                    .gecos("Application Token")
                    .mail(auth.getPrincipal().toString())
                    .build();
        } else {
            creator = peopleGroupService.findPerson(auth);
        }
        entryService.createNew(
                EntryNewDTO.builder()
                        .logbooks(Collections.singletonList(logbookDTO.id()))
                        .title(documentFileName==null?"Printed Text Document":documentFileName)
                        .text(documentContent.toString())
                        .build(),
                creator
        );
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
