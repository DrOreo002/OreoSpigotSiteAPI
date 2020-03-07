package me.droreo002.site.spigot;

import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.interfaces.User;
import me.droreo002.site.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class SpigotMasterUser extends SpigotObject implements User {

    @Getter
    private String userName;
    @Getter
    private String loginToken;
    @Getter
    private String profileImageUrl;

    private List<SpigotResource> purchasedResource;

    public SpigotMasterUser(@NotNull Document objectDocument) {
        super(objectDocument);
    }

    @Override
    public void update(@NotNull Document newDocument, String objectUrl) {
        validate(newDocument);
        this.userName = newDocument.select("a.username.NoOverlay").first().text();
        this.loginToken = newDocument.select("input[name=_xfToken]").get(0).attr("value");
        this.purchasedResource = new ArrayList<>();

        // Go back to main profile page and do the rest
        try {
            newDocument = SpigotSite.getInstance().getSpigotUserManager().getObject(userName).get().getObjectDocument();
            validate(newDocument);
            this.profileImageUrl = newDocument.getElementsByClass("avatarScaler").first().getElementsByTag("img").first().absUrl("src");
        } catch (Exception e) {
            e.printStackTrace();
        }
        setId(Integer.parseInt(StringUtils.getStringBetween(newDocument.html(), "member\\?user_id=(.*?)\">")));
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
