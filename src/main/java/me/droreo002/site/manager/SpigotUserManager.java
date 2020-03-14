package me.droreo002.site.manager;

import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotObject;
import me.droreo002.site.spigot.user.SpigotUser;
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
            SpigotUser user = getCachedObject(objectId);
            if (user == null) {
                String url = getObjectSubUrl() + objectId;
                SpigotObject.validate(url);
                user = new SpigotUser(url, objectId);
            }
            addCache(user);
            startCacheUpdater();
            return user;
        });
    }

    @Override
    @SneakyThrows
    public @Nullable Future<SpigotUser> getObject(String objectName) {
        return THREAD_POOL.submit(() -> {
            Document document = getObjectDocument("?username=" + objectName).get();
            String profileLink = document.getElementsByClass("topLink").first().getElementsByTag("a").first().attr("href");
            return getObject(Integer.parseInt(profileLink.split("/")[2].replace(objectName.toLowerCase() + ".", ""))).get();
        });
    }

    @Override
    @SneakyThrows
    public @Nullable Future<Document> getObjectDocument(String targetUrl) {
        return THREAD_POOL.submit(() -> {
            String url = getObjectSubUrl() + targetUrl;
            Document document = SpigotSite.getInstance().getDocument(url).get();
            SpigotObject.validate(document);
            return document;
        });
    }
}
