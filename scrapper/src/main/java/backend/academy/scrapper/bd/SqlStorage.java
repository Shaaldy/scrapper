package backend.academy.scrapper.bd;

import backend.academy.scrapper.api.ListLinksResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Repository
@Profile("db")
public class SqlStorage implements StorageProvider {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    public SqlStorage(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Добавляет пользователя.
     * @return true если пользователь добавлен, false если уже существовал
     */
    @Override
    public boolean addUser(Long chatId) {
        String sql = """
            INSERT INTO users (id)
            VALUES (:id)
            ON CONFLICT (id) DO NOTHING
        """;

        int rowsAffected = jdbcTemplate.update(sql, Map.of("id", chatId));
        return rowsAffected > 0;
    }

    @Override
    public boolean removeUser(Long chatId) {
        String sql = "DELETE FROM users WHERE id = :id";
        int rowsAffected = jdbcTemplate.update(sql, Map.of("id", chatId));
        return rowsAffected > 0;
    }

    @Override
    public ListLinksResponse getLinks(Long chatId) {
        String sql = """
            SELECT l.url
            FROM links l
            JOIN user_links ul ON l.id = ul.link_id
            WHERE ul.user_id = :user_id
        """;

        var urls = jdbcTemplate.queryForList(sql, Map.of("user_id", chatId), String.class);
        return new ListLinksResponse(chatId, urls); // предполагаю, что у тебя есть такой класс
    }

    @Override
    public void addLink(Long chatId, String url) {
        // 1. Убедимся, что ссылка есть в таблице links
        String insertLinkSql = """
            INSERT INTO links (url)
            VALUES (:url)
            ON CONFLICT (url) DO NOTHING
            RETURNING id
        """;

        Long linkId;
        var params = Map.of("url", url);

        var linkIds = jdbcTemplate.queryForList(insertLinkSql, params, Long.class);

        if (linkIds.isEmpty()) {
            // если ссылка уже есть — достаем id
            String selectIdSql = "SELECT id FROM links WHERE url = :url";
            linkId = jdbcTemplate.queryForObject(selectIdSql, params, Long.class);
        } else {
            linkId = linkIds.get(0);
        }

        // 2. Привяжем ссылку к пользователю
        String insertUserLinkSql = """
            INSERT INTO user_links (user_id, link_id)
            VALUES (:user_id, :link_id)
            ON CONFLICT DO NOTHING
        """;

        jdbcTemplate.update(insertUserLinkSql, Map.of(
            "user_id", chatId,
            "link_id", linkId
        ));
    }

    @Override
    public boolean removeLink(Long chatId, String url) {
        String sql = """
            DELETE FROM user_links
            WHERE user_id = :user_id
              AND link_id = (SELECT id FROM links WHERE url = :url)
        """;

        int rowsAffected = jdbcTemplate.update(sql, Map.of(
            "user_id", chatId,
            "url", url
        ));
        return rowsAffected > 0;
    }

    @Override
    public Set<Long> getSubscribers(String url) {
        String sql = """
            SELECT user_id
            FROM user_links
            WHERE link_id = (SELECT id FROM links WHERE url = :url)
        """;

        return Set.copyOf(jdbcTemplate.queryForList(sql, Map.of("url", url), Long.class));
    }

    @Override
    public Set<String> getAllTrackedLinks() {
        String sql = "SELECT url FROM links";
        return Set.copyOf(jdbcTemplate.queryForList(sql, Map.of(), String.class));
    }



    @Override
    public boolean updateTime(String url, LocalDateTime time) {
        // Попробуем вставить новую запись, если её нет
        String insertSql = """
        INSERT INTO url_updates (url, updated_at)
        VALUES (:url, :time)
        ON CONFLICT (url) DO NOTHING
    """;
        int inserted = jdbcTemplate.update(insertSql, Map.of(
            "url", url,
            "time", time
        ));

        if (inserted > 0) {
            // Запись была создана — обновление произошло
            return true;
        }

        // Если запись уже есть, обновляем только если новое время больше текущего
        String updateSql = """
        UPDATE url_updates
        SET updated_at = :time
        WHERE url = :url AND updated_at < :time
    """;
        int updated = jdbcTemplate.update(updateSql, Map.of(
            "url", url,
            "time", time
        ));

        return updated > 0;
    }


}
