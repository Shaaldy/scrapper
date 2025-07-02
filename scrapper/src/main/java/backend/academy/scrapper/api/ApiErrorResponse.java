package backend.academy.scrapper.api;

import java.util.ArrayList;

public record ApiErrorResponse(String description, String code, String exceptionName, String exceptionMessage,
                               ArrayList<String> stackTrace) {
}

