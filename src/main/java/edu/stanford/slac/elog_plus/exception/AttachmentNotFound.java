package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Attachment has not been found")
public class AttachmentNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "attachmentNotFoundBuilder")
    public AttachmentNotFound(Integer errorCode, String attachmentID, String errorDomain) {
        super(errorCode, String.format("The Attachment '%s' has not been found", attachmentID), errorDomain);
    }
}
