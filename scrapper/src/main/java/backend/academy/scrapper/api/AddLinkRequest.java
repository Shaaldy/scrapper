package backend.academy.scrapper.api;

public class AddLinkRequest implements ILinked {
    String link;

    public AddLinkRequest(String link) {
        this.link = link;
    }

    @Override
    public String getLink() {
        return link;
    }
}
