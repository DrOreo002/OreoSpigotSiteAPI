package me.droreo002.site.spigot;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;

public abstract class SpigotObject {

    @Getter
    private Document objectDocument;
    @Getter @Setter
    private String objectUrl;
    @Getter @Setter
    private int id;

    @SneakyThrows
    public SpigotObject(@NotNull String objectUrl, int id) {
        this.objectUrl = objectUrl;
        this.id = id;
        this.objectDocument = SpigotSite.getInstance().getDocument(objectUrl).get();
        update(objectDocument, objectUrl);
    }

    public SpigotObject(@NotNull Document objectDocument) {
        this.objectDocument = objectDocument;
        this.id = 0;
        this.objectUrl = null;
        update(objectDocument, null);
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
            if (objectDocument.getElementsByClass("baseHtml").text().contains("The requested page could not be found.")) {
                throw new NullPointerException("Cannot find object data on " + url);
            }
        } catch (Exception e) {
            throw new NullPointerException("Cannot find object data on " + url + ". Reason is " + e.getMessage());
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
