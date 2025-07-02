package backend.academy.scrapper.api;

import java.util.ArrayList;

public record LinkResponse(Long id, String url, ArrayList<String> tags, ArrayList<String> filters) {}
