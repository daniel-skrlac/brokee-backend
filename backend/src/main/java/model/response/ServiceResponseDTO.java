package model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponseDTO<T> {
    private boolean success = true;
    private String message = "";
    private int statusCode;
    private T data;
}
