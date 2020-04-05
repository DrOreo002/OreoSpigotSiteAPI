package me.droreo002.site.spigot;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

import java.util.concurrent.ExecutionException;

public abstract class SpigotObject {

    @Getter
    private Document objectDocument;
    @Getter @Setter
    private String objectUrl;
    @Getter @Setter
    private int id;

    public SpigotObject(@NotNull String objectUrl, int id) throws ExecutionException, InterruptedException {
        this(SpigotSite.getInstance().getDocument(objectUrl).get(), objectUrl, id);
    }

    public SpigotObject(@NotNull Document objectDocument) {
        this(objectDocument, null, 0);
    }

    public SpigotObject(@NotNull Document objectDocument, @Nullable String objectUrl) {
        this(objectDocument, objectUrl, 0);
    }

    public SpigotObject(@NotNull Document objectDocument, @Nullable String objectUrl, int id) {
        this.objectDocument = objectDocument;
        this.objectUrl = objectUrl;
        this.id = id;
        update(objectDocument, objectUrl);
    }

    /**
     * Update this object by default
     * value
     */
    public void update() {
        try {
            if (getObjectUrl() == null) throw new NullPointerException("Object url cannot be null!");
            this.update(SpigotSite.getInstance().getDocument(getObjectUrl()).get(), getObjectUrl());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update this site object
     *
     * @param objectDocument The document
     */
    public abstract void update(@NotNull Document objectDocument, @Nullable String objectUrl);

    /**
     * Validate this url that the url is
     * a valid site object
     *
     * @param url The object url to validate
     */
    public static void validate(String url) {
        if (url == null) return;
        try {
            Document objectDocument = SpigotSite.getInstance().getDocument(url).get();
            if (objectDocument == null) throw new NullPointerException("Cannot find object data on " + url);
        } catch (Exception e) {
            System.out.println("Failed to get object data on " + url);
            e.printStackTrace();
        }
    }

    /**
     * Validate this document that the document
     * is a valid site object
     *
     * @param document The target document
     */
    public static void validate(Document document) {
        if (document.getElementsByClass("baseHtml").text().contains("The requested page could not be found.")) {
            throw new NullPointerException("Document is invalid for this object data!");
        }
    }
}
