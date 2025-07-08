package backend.academy.scrapper;

import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

public record ApiErrorResponse(String description, String code, String exceptionName, String exceptionMessage,
                               ArrayList<String> stackTrace) {
    public @NotNull String toString() {
        return description + "\n" + code + "\n" + exceptionName + "\n" + exceptionMessage;
    }
}

