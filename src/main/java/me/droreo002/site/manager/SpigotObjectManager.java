package me.droreo002.site.manager;

import lombok.Getter;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotObject;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class SpigotObjectManager<T extends SpigotObject> {

    public static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    @Getter
    private SpigotSite spigotSite;
    @Getter
    private String objectSubUrl;

    public SpigotObjectManager(SpigotSite spigotSite, String objectSubUrl) {
        this.objectSubUrl = SpigotSite.SPIGOT_URL + "/" + objectSubUrl;
        this.spigotSite = spigotSite;
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
}
