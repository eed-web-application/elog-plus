package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.CONFLICT, reason = "TagAlreadyExists")
public class TagAlreadyExists extends ControllerLogicException {
    @Builder(builderMethodName = "tagAlreadyExistsBuilder")
    public TagAlreadyExists(Integer errorCode, String tagName, String errorDomain) {
        super(errorCode, String.format("A tag with the name '%s' already exists", tagName), errorDomain);
    }
}
