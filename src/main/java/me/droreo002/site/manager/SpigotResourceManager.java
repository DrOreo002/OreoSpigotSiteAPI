package me.droreo002.site.manager;

import me.droreo002.site.SpigotSite;
import me.droreo002.site.exceptions.NotSupportedException;
import me.droreo002.site.spigot.resource.SpigotPremiumResource;
import me.droreo002.site.spigot.resource.SpigotResource;
import me.droreo002.site.utils.SiteUtils;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

import java.util.concurrent.Future;

public class SpigotResourceManager extends SpigotObjectManager<SpigotResource> {

    public SpigotResourceManager(SpigotSite spigotSite) {
        super(spigotSite,"resources/");
    }

    @Override
    public @Nullable Future<SpigotResource> getObject(int objectId) {
        return THREAD_POOL.submit(() -> {
            String url = getObjectSubUrl() + objectId;
            SpigotResource resource = getCachedObject(objectId);
            if (resource == null) {
                SpigotResource.Category category = SiteUtils.getResourceCategory(url);
                if (category == SpigotResource.Category.PREMIUM) {
                    resource = new SpigotPremiumResource(url, objectId);
                } else {
                    resource = new SpigotResource(url, objectId);
                }
            }
            addCache(resource);
            startCacheUpdater(); // Only start after done adding at least 1 item
            return resource;
        });
    }

    @Override
    public @Nullable Future<SpigotResource> getObject(String objectName) {
        throw new NotSupportedException("Get object by name is not supported in here!");
    }

    @Override
    public @Nullable Future<Document> getObjectDocument(String targetUrl) {
        return THREAD_POOL.submit(() -> {
            String url = getObjectSubUrl() + targetUrl;
            return SpigotSite.getInstance().getDocument(url).get();
        });
    }
}
