package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "ItemNotFound")
public class ItemNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "ItemNotFoundBuilder")
    public ItemNotFound(Integer errorCode, String errorMessage, String errorDomain) {
        super(errorCode, errorMessage, errorDomain, HttpStatus.NOT_FOUND);
    }
}
