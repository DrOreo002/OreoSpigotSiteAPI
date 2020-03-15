package me.droreo002.site.spigot.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.droreo002.site.spigot.SpigotObject;
import me.droreo002.site.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SpigotUser extends SpigotObject {

    @Getter
    private String userName;
    @Getter
    private String profileImageUrl;
    @Getter
    private ProfilePost lastProfilePost;

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
        Element firstProfilePost = newDocument.select("ol.messageSimpleList li[id*=profile-post]").first();
        this.lastProfilePost = new ProfilePost(
                firstProfilePost.select("article blockquote").text(),
                firstProfilePost.select("div.messageContent a[class*=username]").text(),
                firstProfilePost.select("div.messageMeta span.DateTime").attr("title")
        );
    }

    /**
     * Get the user id
     *
     * @return The user id
     */
    public int getUserId() {
        return this.getId();
    }

    @Getter
    @AllArgsConstructor
    public static class ProfilePost {
        private String message;
        private String author;
        private String date;

        @Override
        public String toString() {
            return "ProfilePost{" +
                    "message='" + message + '\'' +
                    ", author='" + author + '\'' +
                    ", date='" + date + '\'' +
                    '}';
        }
    }
}
