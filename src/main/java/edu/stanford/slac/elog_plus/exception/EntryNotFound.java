package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "EntryNotFound")
public class EntryNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "entryNotFoundBuilder")
    public EntryNotFound(Integer errorCode, String errorDomain) {
        super(errorCode, "The entry has not been found", errorDomain);
    }

    @Builder(builderMethodName = "entryNotFoundBuilderWithName")
    public EntryNotFound(Integer errorCode, String entryName, String errorDomain) {
        super(errorCode, "The entry '%s' has not been found", entryName);
    }
}
