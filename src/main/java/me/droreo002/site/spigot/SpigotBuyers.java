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

    public SpigotBuyers(@NotNull Document objectDocument) {
        super(objectDocument);
    }

    @Override
    public void update(@NotNull Document objectDocument, @Nullable String objectUrl) {
        this.buyerCredentials = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy 'at' hh:mm a", Locale.ENGLISH);
        Elements buyersBlocks = objectDocument.select(".memberListItem");
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
    }
}