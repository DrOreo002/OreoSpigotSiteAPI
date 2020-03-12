package me.droreo002.site.utils;

import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotResource;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public final class SiteUtils {

    @SneakyThrows
    public static SpigotResource.Category getResourceCategory(String resourceUrl) {
        Document document = SpigotSite.getInstance().getDocument(resourceUrl).get();
        Element sideResourceInfo = document.getElementsByClass("uix_mainSidebar").first().getElementById("resourceInfo").getElementsByClass("secondaryContent").first();
        return SpigotResource.Category.match(sideResourceInfo.select(".resourceCategory").first().getElementsByTag("a").first().text());
    }
}
