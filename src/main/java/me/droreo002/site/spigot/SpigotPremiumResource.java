package me.droreo002.site.spigot;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class SpigotPremiumResource extends SpigotResource {

    @Getter
    private List<SpigotUser> buyers;

    public SpigotPremiumResource(@NotNull String objectUrl, int id) {
        super(objectUrl, id);
        this.buyers = new ArrayList<>();
    }

    @Override
    public void update(@NotNull Document objectDocument, String objectUrl) {
        super.update(objectDocument, objectUrl);
        if (objectDocument.hasClass("resourceTabBuyers ")) {

        }
    }
}
