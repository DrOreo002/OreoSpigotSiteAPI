package me.droreo002.site.spigot.user;

import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.resource.SpigotResource;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SpigotMasterUser extends SpigotUser {

    @Getter
    private String loginToken;

    private List<SpigotResource> purchasedResource;

    public SpigotMasterUser(@NotNull Document objectDocument) {
        super(objectDocument);
    }

    @Override
    @SneakyThrows
    public void update(@NotNull Document newDocument, String objectUrl) {
        validate(newDocument);
        this.loginToken = newDocument.select("input[name=_xfToken]").get(0).attr("value");
        this.purchasedResource = new ArrayList<>();
        super.update(SpigotSite.getInstance().getSpigotUserManager().getObjectDocument(newDocument.selectFirst("a[class=username NoOverlay]")
                .attr("href").split("\\.")[1].replace("/", "")).get(), objectUrl);
    }

    @SneakyThrows
    public List<SpigotResource> getPurchasedResource() {
        if (this.purchasedResource.isEmpty()) {
            Document document = SpigotSite.getInstance().getDocument("https://www.spigotmc.org/resources/purchased").get();
            Elements resourceList = document.getElementsByClass("resourceList").first().children();
            for (Element resource : resourceList) {
                int resourceId = Integer.parseInt(resource.attr("id").replace("resource-", ""));
                this.purchasedResource.add(SpigotSite.getInstance().getSpigotResourceManager().getObject(resourceId).get());
            }
        }
        return purchasedResource;
    }

    @Override
    public int getUserId() {
        return this.getId();
    }
}
