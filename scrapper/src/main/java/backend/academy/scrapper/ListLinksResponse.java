package backend.academy.scrapper;

import java.util.HashSet;
import java.util.Set;

public class ListLinksResponse {
    Set<LinkResponse> links;
    Integer size;

    public ListLinksResponse(Set<LinkResponse> links) {
        this.links = links;
        this.size = links.size();
    }

    public ListLinksResponse() {
        this.links = new HashSet<>();
        this.size = 0;
    }

    public void addLink(LinkResponse link) {
        this.links.add(link);
    }

    public boolean deleteLink(String linked) {
        boolean res = false;
        for (LinkResponse link : links) {
            if (link.url().equals(linked)) {
                links.remove(link);
                res = true;
            }
        }
        this.size = links.size();
        return res;
    }


    public Set<LinkResponse> getLinks() {
        return links;
    }

    public void setLinks(Set<LinkResponse> links) {
        this.links = links;
    }

    public Integer getSize() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
