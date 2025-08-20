package backend.academy.scrapper.service;

import java.time.LocalDateTime;

public record UpdateInfo(
    boolean hasUpdates,
    String type,          // "REPO", "PR", "ISSUE", "SO"
    GithubBase answer,  // null для SO
    LocalDateTime updatedAt
) {}
