package me.droreo002.site.spigot;

import lombok.Getter;
import me.droreo002.site.SpigotSite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpigotResourceUpdate extends SpigotObject {

    @Getter
    private String updateLink;
    @Getter
    private String updateTitle;
    @Getter
    private String updateArticleHtml;

    public SpigotResourceUpdate(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    @Override
    public void update(@NotNull Document objectDocument, @Nullable String objectUrl) {
        Elements resourceBlocks = objectDocument.select("li.primaryContent");
        for (Element resourceBlock : resourceBlocks) {
            setId(Integer.parseInt(resourceBlock.attr("id").replace("update-", "")));
            this.updateLink = SpigotSite.SPIGOT_URL + resourceBlock.select("h2.textHeading a").first().attr("href");
            this.updateTitle = resourceBlock.select("h2.textHeading a").first().text();
            this.updateArticleHtml = resourceBlock.select("article blockquote").first().text();
        }
    }
}
