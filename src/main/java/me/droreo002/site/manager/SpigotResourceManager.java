package me.droreo002.site.manager;

import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.exceptions.NotSupportedException;
import me.droreo002.site.spigot.SpigotPremiumResource;
import me.droreo002.site.spigot.SpigotResource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SpigotResourceManager extends SpigotObjectManager<SpigotResource> {

    @Getter
    private List<SpigotResource> cachedResources;

    public SpigotResourceManager(SpigotSite spigotSite) {
        super(spigotSite,"resources/");
        this.cachedResources = new CopyOnWriteArrayList<>();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if (cachedResources.isEmpty()) return;
                List<Integer> ids = cachedResources.stream().map(SpigotResource::getId).collect(Collectors.toList());
                cachedResources.clear();
                ids.forEach(i -> {
                    try {
                        getObject(i).get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
            }
        }, 0L, TimeUnit.MINUTES.toMillis(1));
    }

    @Override
    public @Nullable Future<SpigotResource> getObject(int objectId) {
        return THREAD_POOL.submit(() -> {
            String url = getObjectSubUrl() + objectId;
            SpigotResource resource = getCachedResource(objectId);
            if (resource == null) {
                resource = new SpigotResource(url, objectId);
                if (resource.getCategory() == SpigotResource.Category.PREMIUM) resource = new SpigotPremiumResource(url, objectId);
            }
            return resource;
        });
    }

    @Override
    public @Nullable Future<SpigotResource> getObject(String objectName) {
        throw new NotSupportedException("Get object by name is not supported in here!");
    }

    /**
     * Get spigot resource in cached
     *
     * @param resourceId The resource id
     * @return SpigotResource
     */
    @Nullable
    public SpigotResource getCachedResource(int resourceId) {
        return this.cachedResources.stream().filter(resource -> resource.getId() == resourceId).findAny().orElse(null);
    }
}
