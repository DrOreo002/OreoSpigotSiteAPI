package me.droreo002.site.manager;

import lombok.Getter;
import lombok.Setter;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotObject;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class SpigotObjectManager<T extends SpigotObject> {

    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    public static final ExecutorService UPDATER_THREAD_POOL = Executors.newSingleThreadExecutor();

    @Getter
    private SpigotSite spigotSite;
    @Getter
    private String objectSubUrl;
    @Getter
    private List<T> cachedObjects;
    @Getter
    private boolean cacheUpdaterStarted;
    @Getter @Setter
    private Callable<Void> onCacheUpdated;

    public SpigotObjectManager(SpigotSite spigotSite, String objectSubUrl) {
        this.objectSubUrl = SpigotSite.SPIGOT_URL + "/" + objectSubUrl;
        this.spigotSite = spigotSite;
        this.cachedObjects = new CopyOnWriteArrayList<>();
    }

    public void startCacheUpdater() {
        if (this.cacheUpdaterStarted) return;
        this.cacheUpdaterStarted = true;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                UPDATER_THREAD_POOL.submit(() -> updateObjects());
            }
        }, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(1));
    }

    /**
     * Update the objects
     * Warning: Run on the main thread
     */
    public void updateObjects() {
        if (cachedObjects.isEmpty()) return;
        List<Integer> ids = cachedObjects.stream().map(SpigotObject::getId).collect(Collectors.toList());
        cachedObjects.clear();
        ids.forEach(i -> {
            try {
                getObject(i).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        try {
            if (onCacheUpdated != null) onCacheUpdated.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the object by object id
     *
     * @param objectId The object id
     * @return The object if available, null otherwise
     */
    @Nullable
    public abstract Future<T> getObject(int objectId);

    /**
     * Get the object by object name
     *
     * @param objectName The object name
     * @return The object if available, null otherwise
     */
    @Nullable
    public abstract Future<T> getObject(String objectName);

    /**
     * Get the object document
     *
     * @param targetUrl The object's target url
     * @return The object document
     */
    @Nullable
    public abstract Future<Document> getObjectDocument(String targetUrl);

    /**
     * Add that object into cache
     *
     * @param object The object
     */
    public void addCache(T object) {
        this.cachedObjects.removeIf(o -> o.getId() == object.getId());
        this.cachedObjects.add(object);
    }

    /**
     * Get spigot resource in cached
     *
     * @param objectId The resource id
     * @return SpigotResource
     */
    @Nullable
    public T getCachedObject(int objectId) {
        return this.cachedObjects.stream().filter(resource -> resource.getId() == objectId).findAny().orElse(null);
    }
}
