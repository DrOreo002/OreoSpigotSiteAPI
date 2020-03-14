package me.droreo002.site.spigot.resource;

import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotBuyers;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SpigotPremiumResource extends SpigotResource {

    @Getter
    private SpigotBuyers buyers; // Can be null if this premium resource is not owned by master user
    @Getter
    private String price;

    public SpigotPremiumResource(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    @SneakyThrows
    @Override
    public void update(@NotNull Document objectDocument, String objectUrl) {
        super.update(objectDocument, objectUrl);
        Elements downloadButton = objectDocument.select("label.downloadButton");
        if (downloadButton.text().contains("Download Now")) {
            String authorUrl = "https://www.spigotmc.org/resources/authors/" + getAuthor().combine();
            System.out.println("Checking price: " + authorUrl);
            Document authorResources = SpigotSite.getInstance().getDocument(authorUrl).get();
            Elements list = authorResources.select("ol.resourceList li");
            for (Element el : list) {
                if (el.attr("id").contains(String.valueOf(getId()))) {
                    this.price = el.select("span.cost").text();
                    break;
                }
            }
        } else {
            this.price = downloadButton.text().replace(getJarSize() + " .jar", "").split("for")[1];
        }

        // Update buyer
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
