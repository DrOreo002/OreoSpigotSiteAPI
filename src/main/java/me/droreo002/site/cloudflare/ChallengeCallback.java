package me.droreo002.site.cloudflare;

import org.jsoup.nodes.Document;

public interface ChallengeCallback {
    void onSuccess(Document document);
    void onFailure();
}
