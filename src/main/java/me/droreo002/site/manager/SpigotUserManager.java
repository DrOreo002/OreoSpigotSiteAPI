package me.droreo002.site.manager;

import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotObject;
import me.droreo002.site.spigot.SpigotUser;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

import java.util.concurrent.Future;

public class SpigotUserManager extends SpigotObjectManager<SpigotUser> {

    public SpigotUserManager(SpigotSite spigotSite) {
        super(spigotSite, "members/");
    }

    @Override
    public @Nullable Future<SpigotUser> getObject(int objectId) {
        return THREAD_POOL.submit(() -> {
            String url = getObjectSubUrl() + objectId;
            SpigotObject.validate(url);
            return new SpigotUser(url, objectId);
        });
    }

    @Override
    @SneakyThrows
    public @Nullable Future<SpigotUser> getObject(String objectName) {
        return THREAD_POOL.submit(() -> {
            String url = getObjectSubUrl() + "?username=" + objectName;
            Document document = SpigotSite.getInstance().getDocument(url).get();
            SpigotObject.validate(url);
            String profileLink = document.getElementsByClass("topLink").first().getElementsByTag("a").first().attr("href");
            return new SpigotUser(url, Integer.parseInt(profileLink.split("/")[2].replace(objectName.toLowerCase() + ".", "")));
        });
    }
}
