package edu.stanford.slac.elog_plus.api.v1.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResultResponse<T> {
    @Builder.Default
    @Schema(description = "Is the error code returned from api", requiredMode = Schema.RequiredMode.REQUIRED)
    private int errorCode = 0;
    @Schema(description = "In case of error not equal to 0, an error message can be reported by api, indicating what problem is occured")
    private String errorMessage;
    @Schema(description = "In case of error not equal to 0, an error domain can be reported by api, indicating where the problem is occured")
    private String errorDomain;
    @Schema(description = "Is the value returned by api")
    private T payload;

    /**
     * Fast constructor for error situation
     *
     * @param errorCode    the error code
     * @param errorMessage the error message
     * @param errorDomain  the error domani, where the error is occurs
     * @param <T>          the type
     * @return return the instance with error information
     */
    public static <T> ApiResultResponse<T> of(
            int errorCode,
            String errorMessage,
            String errorDomain) {
        return ApiResultResponse
                .<T>builder()
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .errorDomain(errorDomain)
                .build();
    }

    /**
     * Fast constructor for good result situation
     *
     * @param payload the payload of the result
     * @param <T>     the generics type
     * @return return the isntance with payload
     */
    public static <T> ApiResultResponse<T> of(T payload) {
        return ApiResultResponse
                .<T>builder()
                .payload(payload)
                .build();
    }

    //---------------- keep this with depecrated annotation
    @Deprecated()
    public ApiResultResponse(T payload) {
        this.payload = payload;
    }

    @Deprecated
    public ApiResultResponse(int errorCode, String errorMessage, String errorDomain) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDomain = errorDomain;
    }

    @Deprecated
    public ApiResultResponse(T payload, int errorCode, String errorMessage, String errorDomain) {
        this.payload = payload;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.errorDomain = errorDomain;
    }
}
