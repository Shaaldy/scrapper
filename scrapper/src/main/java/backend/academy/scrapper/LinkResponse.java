package backend.academy.scrapper;

public class LinkResponse implements ILinked {
    private Long id;
    private String url;

    public  LinkResponse(Long id, String url) {
        this.id = id;
        this.url = url;
    }

    @Override
    public String getLink() {
        return url;
    }

}
