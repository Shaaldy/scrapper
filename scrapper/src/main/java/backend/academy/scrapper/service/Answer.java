package backend.academy.scrapper.service;

public enum Answer {
    SO,
    SO_REQUEST,
    SO_COMMENT,
    REPO,
    PR,
    ISSUE;

    public boolean isGITHUB(){
        return this == REPO || this == PR || this == ISSUE;
    }

    public boolean isSO(){
        return this == SO || this == SO_REQUEST || this == SO_COMMENT;
    }

}
