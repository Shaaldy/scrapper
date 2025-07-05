package backend.academy.scrapper;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;

public record ApiErrorResponse(String description, String code, String exceptionName, String exceptionMessage,
                               ArrayList<String> stackTrace) {
    public @NotNull String toString() {
        return description + "\n" + code + "\n" + exceptionName + "\n" + exceptionMessage;
    }
}

