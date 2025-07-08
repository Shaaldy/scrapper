package backend.academy.bot;

import backend.academy.BotApplication;
import org.springframework.boot.SpringApplication;

public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.from(BotApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
