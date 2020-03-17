package me.droreo002.site.spigot.resource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotObject;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import org.jetbrains.annotations.Nullable;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SpigotResource extends SpigotObject {

    @Getter
    private String name, lastUpdateDate, tag, firstRelease, version, iconUrl, jarSize;
    @Getter
    private int totalDownloads;
    @Getter
    private Category category;
    @Getter
    private List<SpigotResourceUpdate> resourceUpdates;
    @Getter
    private Author author;

    public SpigotResource(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
    }

    @SneakyThrows
    @Override
    public void update(@NotNull Document objectDocument, String objectUrl) {
        validate(objectUrl);
        this.resourceUpdates = new CopyOnWriteArrayList<>();
        Element resourceInfo = objectDocument.getElementsByClass("resourceInfo").first();
        this.version = resourceInfo.select("h1").first().select(".muted").first().text();
        this.iconUrl = resourceInfo.getElementsByClass("resourceImage").first().getElementsByClass("resourceIcon").first().absUrl("src");
        this.name = resourceInfo.select("h1").first().text();
        this.tag = resourceInfo.getElementsByClass("tagLine muted").first().text();
        Element sideResourceInfo = objectDocument.getElementsByClass("uix_mainSidebar").first().getElementById("resourceInfo").getElementsByClass("secondaryContent").first();
        this.lastUpdateDate = sideResourceInfo.select("dl.lastUpdate").text().replace("Last Update: ", "");
        this.category = Category.match(sideResourceInfo.select(".resourceCategory").first().getElementsByTag("a").first().text());
        this.firstRelease = sideResourceInfo.select(".firstRelease").text();
        this.totalDownloads = Integer.parseInt(sideResourceInfo.select(".downloadCount").first().getElementsByTag("dd").first().text().replace(",", ""));
        this.jarSize = objectDocument.select("label.downloadButton small.minorText").first().text().replace(" .jar", "");

        Element authorElement = sideResourceInfo.select(".author").select("a").first();
        this.author = new Author(Integer.parseInt(authorElement.attr("href").split("\\.")[1].replace("/", "")), authorElement.text());

        updateResourceUpdates(objectUrl);
    }

    /**
     * Update the resource update list
     *
     * @param objectUrl The object url
     */
    @SneakyThrows
    public void updateResourceUpdates(String objectUrl) {
        // Resource update
        resourceUpdates.clear();
        String updateListUrl = objectUrl + "/updates";
        Document updateDocument = SpigotSite.getInstance().getDocument(updateListUrl).get();
        Elements pages = updateDocument.select("div.PageNav nav a");
        if (pages.size() != 0) {
            for (Element page : pages) {
                String newUrl = SpigotSite.SPIGOT_URL + "/" + page.attr("href");
                Document updatePage = SpigotSite.getInstance().getDocument(newUrl).get();
                Elements elements = updatePage.select("div.updateContainer ol li.primaryContent");
                for (int i = 0; i < 5; i++) {
                    try {
                        Element uPage = elements.get(i);
                        int updateId;
                        try {
                            updateId = Integer.parseInt(uPage.attr("id").replace("update-", ""));
                        } catch (NumberFormatException e) {
                            System.out.println("Error reading update on " + uPage.html());
                            continue;
                        }
                        String updateDirectUrl = objectUrl + "/update?" + "update=" + updateId;
                        resourceUpdates.add(new SpigotResourceUpdate(updateDirectUrl, 0));
                    } catch (IndexOutOfBoundsException e) {
                        break;
                    }
                }
                break; // Only loop for first page
            }
        } else {
            resourceUpdates.add(new SpigotResourceUpdate(updateListUrl, 0));
        }
    }

    /**
     * Get the update by id
     *
     * @param updateId The update id
     * @return SpigotResourceUpdate if found
     */
    @Nullable
    public SpigotResourceUpdate getUpdateById(int updateId) {
        SpigotResourceUpdate resourceUpdate = this.resourceUpdates.stream().filter(update -> update.getId() == updateId).findAny().orElse(null);
        if (resourceUpdate == null) {
            String updateDirectUrl = getObjectUrl() + "/resourceUpdate?" + "resourceUpdate=" + updateId;
            try {
                return new SpigotResourceUpdate(updateDirectUrl, 0);
            } catch (Exception e) {
                return null;
            }
        }
        return resourceUpdate;
    }

    /**
     * Get the latest update for this resource
     *
     * @return SpigotResourceUpdate if found
     */
    @Nullable
    public SpigotResourceUpdate getLatestUpdate() {
        try {
            return this.resourceUpdates.get(0);
        } catch (Exception e) { return null; }
    }

    public enum Category {

        /*
        Spigot plugin that interacts with BungeeCord
         */
        BUNGEE_SPIGOT("Bungee - Spigot"),

        /**
         * Bungee plugin
         */
        BUNGEE_PROXY("Bungee - Proxy"),

        /**
         * Spigot plugin
         */
        SPIGOT("Spigot"),

        /**
         * Standalone application (Not website)
         */
        STANDALONE("Standalone"),

        /**
         * Premium, paid addons
         */
        PREMIUM("Premium"),

        /**
         * Plugin that can operate on BungeeCord and Spigot
         */
        UNIVERSAL("Universal"),

        /**
         * Standalone HTML, CSS, JavaScript, etc
         */
        WEB("Web");

        @Getter
        private String asString;

        Category(String asString) {
            this.asString = asString;
        }

        /**
         * Match a category by that string
         * @param asString The string
         * @return Category, can be null
         */
        @NotNull
        public static Category match(String asString) {
            for (Category c : values()) {
                if (c.asString.equalsIgnoreCase(asString)) return c;
            }
            return Category.SPIGOT; // Default
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Author {
        private int id;
        private String userName;

        /**
         * Get the combined, useful for
         * url search
         *
         * @return Combined id and username
         */
        public String combine() {
            return userName.toLowerCase() + "." + id;
        }
    }
}
