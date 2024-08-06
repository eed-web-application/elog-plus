package edu.stanford.slac.elog_plus.api.v1.controller;

import com.hp.jipp.encoding.IppInputStream;
import com.hp.jipp.encoding.IppOutputStream;
import com.hp.jipp.model.Operation;
import com.hp.jipp.model.Status;
import com.hp.jipp.trans.IppPacketData;
import edu.stanford.slac.elog_plus.service.PrinterService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Log4j2
@Validated
@RestController()
@RequestMapping("/v1/printers")
@AllArgsConstructor
@Schema(description = "Api to support IPP printing for the entry creations")
public class PrinterController {
    PrinterService printerService;

    @PostMapping(
            value = "/defaults",
            consumes = {"application/ipp"}
    )
    public ResponseEntity<byte[]> defaultPrinter(
            Authentication authentication,
            @RequestBody byte[] body
    ) throws IOException {
        try {
            // check authentication
            IppInputStream inputStream = new IppInputStream( new ByteArrayInputStream(body));
            IppPacketData data = new IppPacketData(inputStream.readPacket(), inputStream);

            IppPacketData response = null;
            if ((authentication == null || !authentication.isAuthenticated()) &&
                    printerService.getPrintCommand(data) != Operation.Code.getPrinterAttributes) {
                // print job should be authenticated
                response = new IppPacketData(printerService.createErrorResponsePacket(data.getPacket(), Status.clientErrorNotAuthorized));
            } else {
                response = printerService.handleIppPacket(data);
            }
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
            log.error("Error processing IPP request", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
