package backend.academy.scrapper;

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
