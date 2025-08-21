package backend.academy.scrapper.service;

import backend.academy.scrapper.service.clients.ClientAnswer;

public interface IClient {
    ClientAnswer checkForUpdates(String link);
}
