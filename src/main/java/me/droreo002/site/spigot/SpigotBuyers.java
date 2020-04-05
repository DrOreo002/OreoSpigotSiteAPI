package me.droreo002.site.spigot;

import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.user.SpigotUser;
import me.droreo002.site.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SpigotBuyers extends SpigotObject {

    @Getter
    private List<Buyer> buyerCredentials;

    public SpigotBuyers(@NotNull Document objectDocument, @NotNull String objectUrl) {
        super(objectDocument, objectUrl);
    }

    @Override
    @SneakyThrows
    public void update(@NotNull Document objectDocument, @Nullable String objectUrl) {
        this.buyerCredentials = new ArrayList<>();
        // Get all available buyer pages
        Elements pageNav = objectDocument.select("div.PageNav");
        int pages = 1;
        if (pageNav.size() != 0) {
            pages = Integer.parseInt(pageNav.attr("data-last"));
        }
        for (int i = 1; i <= pages; i++) {
            String newUrl = objectUrl + "?page=" + i;
            Document newDoc = SpigotSite.getInstance().getDocument(newUrl).get();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.ENGLISH);
            Elements buyersBlocks = newDoc.select(".memberListItem");
            for (Element buyersBlock : buyersBlocks) {
                Buyer buyer = new Buyer();
                try {
                    Element memberNameBlock = buyersBlock.select("div.member").first();
                    Element purchaseElement = buyersBlock.select("div.muted").first();
                    if (purchaseElement != null) {
                        String purchaseString = purchaseElement.text();
                        if (purchaseString.contains("Purchased")) {
                            String[] data = purchaseString.split("Purchased For:");
                            double price = 0D;
                            try {
                                price = Double.parseDouble(data[1].split(" ")[0]);
                            } catch (Exception ignored) {
                            }
                            String currency = data[1].split(" ")[1];
                            buyer.setPurchaseCurrency(currency);
                            buyer.setPurchasePrice(price);
                        }
                    }
                    try {
                        Element purchaseDateElement = buyersBlock.select(".DateTime.muted").first();
                        if (purchaseDateElement != null) {
                            if (purchaseDateElement.hasAttr("data-time")) {
                                Date date = new Date(Long.parseLong(purchaseDateElement.attr("data-time")) * 1000);
                                buyer.setPurchaseDate(date);
                            } else {
                                String title = purchaseDateElement.attr("title");
                                Date date = sdf.parse(title);
                                buyer.setPurchaseDate(date);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    Elements userNameElements = memberNameBlock.select("a.username");
                    if (userNameElements.size() == 0) {
                        continue;
                    }
                    Element userElement = userNameElements.get(0);
                    buyer.setUsername(userElement.text());
                    String userIdStr = StringUtils.getStringBetween(userElement.attr("href"), "\\.(.*?)/");
                    if (userIdStr.equals("")) {
                        userIdStr = StringUtils.getStringBetween(userElement.attr("href"), "/(.*?)/");
                    }
                    buyer.setUserId(Integer.parseInt(userIdStr));
                    buyerCredentials.add(buyer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Get buyer information from user id
     *
     * @param userId The user id
     * @return Buyer
     */
    @Nullable
    public Buyer getBuyer(int userId) {
        return this.buyerCredentials.stream().filter(b -> b.getUserId() == userId).findAny().orElse(null);
    }

    /**
     * Check if the user id is a buyer
     *
     * @param userId The user id
     * @return true if buyer, false otherwise
     */
    public boolean isBuyer(int userId) {
        return this.buyerCredentials.stream().anyMatch(b -> {
            return b.getUserId() == userId;
        });
    }

    /**
     * Check if the user name is a buyer
     *
     * @param userName The user name
     * @return true if buyer, false otherwise
     */
    public boolean isBuyer(String userName) {
        return this.buyerCredentials.stream().anyMatch(b -> b.getUsername().equalsIgnoreCase(userName));
    }

    @Data
    public static class Buyer {
        private String purchaseCurrency;
        private double purchasePrice;
        private Date purchaseDate;
        private String username;
        private int userId;

        /**
         * Get the formatted purchase price
         *
         * @return Formatted purchase price (Currency) + (Price)
         */
        public String getFormattedPurchasePrice() {
            return purchaseCurrency + " " + purchasePrice;
        }

        /**
         * Get as a SpigotUser
         *
         * @return SpigotUser, can be null
         */
        @SneakyThrows
        @Nullable
        public SpigotUser asSpigotUser() {
            return SpigotSite.getInstance().getSpigotUserManager().getObject(userId).get();
        }

        /**
         * Get the user link
         *
         * @return The user link
         */
        public String getUserLink() {
            return "https://www.spigotmc.org/members/" + username.toLowerCase() + "." + userId;
        }
    }
}