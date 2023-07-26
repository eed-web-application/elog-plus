package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "ControllerLogicException")
public class ControllerLogicException extends RuntimeException {
    private int errorCode;
    private String errorMessage;
    private String errorDomain;
    public ApiResultResponse<Object> toApiResultResponse() {
        return ApiResultResponse.of(this.errorCode, this.errorMessage, this.errorDomain);
    }

    static public <T> ApiResultResponse<T> toApiResultResponse(ControllerLogicException logicException) {
        ApiResultResponse<T> result = new ApiResultResponse<>();
        result.setErrorCode(logicException.getErrorCode());
        result.setErrorMessage(logicException.getErrorMessage());
        result.setErrorDomain(logicException.getErrorDomain());
        return result;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setErrorDomain(String errorDomain) {
        this.errorDomain = errorDomain;
    }

    static public ControllerLogicException of(int errorCode, String errorMessage, String errorDomain) {
        return new ControllerLogicException(errorCode, errorMessage, errorDomain);
    }

    static public ControllerLogicException of(int errorCode, String errorMessage, String errorDomain, Throwable cause) {
        return new ControllerLogicException(errorCode, errorMessage, errorDomain);
    }
}
