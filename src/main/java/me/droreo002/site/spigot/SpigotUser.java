package me.droreo002.site.spigot;

import lombok.Getter;
import me.droreo002.site.interfaces.User;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

public class SpigotUser extends SpigotObject implements User {

    @Getter
    private String userName;
    @Getter
    private String profileImageUrl;

    public SpigotUser(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    @Override
    public void update(@NotNull Document newDocument, String objectUrl) {
        validate(objectUrl);
        this.userName = newDocument.getElementsByClass("username").first().text().replace(" ", "");
        if (newDocument.hasClass("avatarScaler")) {
            this.profileImageUrl = newDocument.getElementsByClass("avatarScaler").first().getElementsByTag("img").first().absUrl("src");
        }
    }

    @Override
    public int getUserId() {
        return this.getId();
    }
}
