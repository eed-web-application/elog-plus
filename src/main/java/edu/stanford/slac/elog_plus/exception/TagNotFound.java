package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "ItemNotFound")
public class TagNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "tagNotFoundBuilder")
    public TagNotFound(Integer errorCode, String tagName, String errorDomain) {
        super(errorCode, String.format("The Tag '%s' has not been found", tagName), errorDomain);
    }
}
