package backend.academy.scrapper;

public class RemoveLinkRequest implements ILinked {
    String url;
    public RemoveLinkRequest(String url) {
        this.url = url;
    }
    @Override
    public String getLink() {
        return url;
    }
}
