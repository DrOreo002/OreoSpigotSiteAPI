package me.droreo002.site.spigot.user;

import lombok.Getter;
import me.droreo002.site.spigot.SpigotObject;
import me.droreo002.site.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

public class SpigotUser extends SpigotObject {

    @Getter
    private String userName;
    @Getter
    private String profileImageUrl;

    public SpigotUser(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    public SpigotUser(@NotNull Document document) {
        super(document);
    }

    @Override
    public void update(@NotNull Document newDocument, String objectUrl) {
        validate(objectUrl);
        this.userName = newDocument.getElementsByClass("username").first().text().replace(" ", "");
        this.profileImageUrl = newDocument.getElementsByClass("avatarScaler").first().getElementsByTag("img").first().absUrl("src");
        setId(Integer.parseInt(StringUtils.getStringBetween(newDocument.html(), "member\\?user_id=(.*?)\">")));
    }

    /**
     * Get the user id
     *
     * @return The user id
     */
    public int getUserId() {
        return this.getId();
    }
}
