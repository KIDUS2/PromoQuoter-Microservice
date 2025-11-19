package et.kifiya.promoquoter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String status;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(String status, String message, T data) {
        this();
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(String status, String message, T data, String path) {
        this();
        this.status = status;
        this.message = message;
        this.data = data;
        this.path = path;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>("CREATED", "Resource created successfully", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("ERROR", message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>("ERROR", message, data);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>("NOT_FOUND", message, null);
    }
}
