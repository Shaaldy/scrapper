package backend.academy.scrapper.api;

import java.util.ArrayList;

public record AddLinkRequest(String link, ArrayList<String> tags, ArrayList<String> filters) {}
