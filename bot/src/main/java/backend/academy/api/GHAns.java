package backend.academy.api;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor
public class GHAns {
    private String type;
    private String url;
    private String description;
    private String author;
    private String updatedAt;

    public boolean isGitHubUpdate() {
        return description != null && author != null && updatedAt != null;
    }

    public String toFormattedMessage() {
        if (isGitHubUpdate()) {
            return String.format(
                "🔔 *Новое обновление в %s *\n\n" +
                    "📝 *Описание:* %s\n" +
                    "👤 *Автор:* %s\n" +
                    "⏰ *Обновлено:* %s\n" +
                    "🔗 *Ссылка:* %s",
                type, description, author, updatedAt, url
            );
        } else {
            return String.format(
                "🔔 *Новое обновление в %s*\n\n" +
                    "🔗 *Ссылка:* %s",
                type,
                url
            );
        }
    }
}
