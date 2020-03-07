package me.droreo002.site.spigot;

import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class SpigotPremiumResource extends SpigotResource {

    @Getter
    private SpigotBuyers buyers; // Can be null if this premium resource is not owned by master user

    public SpigotPremiumResource(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    @SneakyThrows
    @Override
    public void update(@NotNull Document objectDocument, String objectUrl) {
        super.update(objectDocument, objectUrl);
        if (getAuthor().equals(SpigotSite.getInstance().getSpigotMasterUser().getUserName())) {
            String url = objectUrl + "/buyers";
            Document buyerTab = SpigotSite.getInstance().getDocument(url).get();
            // Get all available buyer pages
            Elements pageNav = buyerTab.select("div.PageNav");
            int pages = 1;
            if (pageNav.size() != 0) {
                pages = Integer.parseInt(pageNav.attr("data-last"));
            }
            for (int i = 1; i <= pages; i++) {
                String newUrl = url + "?page=" + i;
                Document newDoc = SpigotSite.getInstance().getDocument(newUrl).get();
                this.buyers = new SpigotBuyers(newDoc);
            }
        }
    }
}
