package backend.academy.api;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record ApiErrorResponse(String description, String code, String exceptionName, String exceptionMessage,
                               List<String> stackTrace) {
    public @NotNull String toString() {
        return description + "\n" + code + "\n" + exceptionName + "\n" + exceptionMessage;
    }
}
