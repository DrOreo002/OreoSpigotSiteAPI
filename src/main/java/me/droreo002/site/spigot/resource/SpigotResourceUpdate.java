package me.droreo002.site.spigot.resource;

import lombok.Getter;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class SpigotResourceUpdate extends SpigotObject {

    @Getter
    private String updateLink;
    @Getter
    private String updateTitle;
    @Getter
    private String updateArticleText;

    public SpigotResourceUpdate(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    @Override
    public void update(@NotNull Document objectDocument, @Nullable String objectUrl) {
        Elements resourceBlock = objectDocument.select("li.primaryContent");
        try {
            setId(Integer.parseInt(resourceBlock.attr("id").replace("update-", "")));
        } catch (NumberFormatException e) {
            System.out.println("Failed to get ID of " + resourceBlock.html());
            e.printStackTrace();
            return;
        }
        this.updateLink = SpigotSite.SPIGOT_URL + "/" + resourceBlock.select("h2.textHeading a").first().attr("href");
        this.updateTitle = resourceBlock.select("h2.textHeading a").first().text();
        this.updateArticleText = resourceBlock.select("article blockquote").text();
    }
}
