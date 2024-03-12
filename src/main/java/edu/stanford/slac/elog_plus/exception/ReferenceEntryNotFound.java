package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "EntryNotFound")
public class ReferenceEntryNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "referenceEntryNotFoundBuilder")
    public ReferenceEntryNotFound(Integer errorCode, String errorDomain) {
        super(errorCode, "The reference entry has not been found", errorDomain);
    }

    @Builder(builderMethodName = "referenceEntryNotFoundBuilderWithName")
    public ReferenceEntryNotFound(Integer errorCode, String entryName, String errorDomain) {
        super(errorCode, "The reference entry '%s' has not been found", entryName);
    }
}
