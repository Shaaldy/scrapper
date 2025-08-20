package backend.academy.scrapper.service;


import java.time.LocalDateTime;

public class GithubBase {

    protected LocalDateTime updatedAt;
    protected String login;
    protected String url;
    protected String description;

    public GithubBase(LocalDateTime updatedAt, String login, String url, String description) {
        this.updatedAt = updatedAt;
        this.login = login;
        this.url = url;
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public String getAuthor() {
        return login;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return url;
    }
}
