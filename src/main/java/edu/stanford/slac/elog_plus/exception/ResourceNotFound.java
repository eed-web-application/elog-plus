package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "the resources has not been found")
public class ResourceNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "notFoundByTypeName")
    public ResourceNotFound(Integer errorCode, String resourceName, String errorDomain) {
        super(errorCode, String.format("The resource '%s' has not been found", resourceName), errorDomain);
    }
    @Builder(builderMethodName = "notFoundByTypeNameAndValue")
    public ResourceNotFound(Integer errorCode, String resourceName, String resourceValue,  String errorDomain) {
        super(errorCode, "The resource '%s' with value '%s' has not been found".formatted(resourceValue, resourceValue), errorDomain);
    }
    @Builder(builderMethodName = "genericBuilder")
    public ResourceNotFound(Integer errorCode, String errorDomain) {
        super(errorCode, "The resource has not been found", errorDomain);
    }
}
