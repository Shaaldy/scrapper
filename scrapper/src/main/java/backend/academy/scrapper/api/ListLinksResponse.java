package backend.academy.scrapper.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListLinksResponse {
    private Set<LinkResponse> links = new HashSet<>();

    public ListLinksResponse(Set<LinkResponse> links) {
        this.links = new HashSet<>(links);
    }

    public ListLinksResponse(Long id, List<String> urls) {
        this.links = new HashSet<>();
        for (String url : urls) {
            this.links.add(new LinkResponse(id, url));
        }
    }

    public ListLinksResponse() {
    }

    public void addLink(LinkResponse link) {
        links.add(link);
    }

    public boolean deleteLink(String linked) {
        return links.removeIf(link -> link.getLink().equals(linked));
    }

    public Set<LinkResponse> getLinks() {
        return links;
    }

    public void setLinks(Set<LinkResponse> links) {
        this.links = new HashSet<>(links);
    }

    public int getSize() {
        return links.size();
    }

    public boolean isEmpty() {
        return links.isEmpty();
    }
}

