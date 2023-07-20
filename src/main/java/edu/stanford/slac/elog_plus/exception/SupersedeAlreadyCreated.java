package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.CONFLICT, reason = "ItemNotFound")
public class SupersedeAlreadyCreated extends ControllerLogicException {
    @Builder(builderMethodName = "supersedeAlreadyCreatedBuilder")
    public SupersedeAlreadyCreated(Integer errorCode, String errorDomain) {
        super(errorCode, "The Entry is already superseded", errorDomain);
    }
}
