import me.droreo002.site.SpigotSite;
import me.droreo002.site.spigot.SpigotBuyers;
import me.droreo002.site.spigot.user.SpigotMasterUser;
import me.droreo002.site.spigot.resource.SpigotPremiumResource;
import me.droreo002.site.spigot.resource.SpigotResource;
import me.droreo002.site.spigot.user.SpigotUser;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class SpigotSiteTest {

    private SpigotSite spigotSite;

    @Before
    public void beforeTest() {
        if (spigotSite == null) {
            try {
                this.spigotSite = SpigotSite.init(new File("H:\\creden.txt"));
            } catch (IllegalStateException e) {
                this.spigotSite = SpigotSite.getInstance();
            }
        }
    }

    @Test
    public void masterUserTest() {
        header("Get master user information");
        try {
            if (spigotSite.getSpigotMasterUser() != null) {
                SpigotMasterUser user = spigotSite.getSpigotMasterUser();
                System.out.println("Master user is enabled");
                logData("Name", user.getUserName());
                logData("Id", String.valueOf(user.getId()));
                logData("Profile Image", user.getProfileImageUrl());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getPremiumResourceTest() {
        header("Get premium resource by ID");
        try {
            SpigotResource resource = spigotSite.getSpigotResourceManager().getObject(75786).get();
            logData("Resource name", resource.getName());
            logData("Resource author", resource.getAuthor().combine());
            logData("Resource downloads", String.valueOf(resource.getTotalDownloads()));
            logData("Resource category", resource.getCategory().getAsString());
            logData("Latest update", String.valueOf(resource.getLatestUpdate().getId()));
            if (resource instanceof SpigotPremiumResource) logData("Resource price", ((SpigotPremiumResource) resource).getPrice());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void header(String s) {
        System.out.println("-----> " + s);
    }

    private void log(String s) {
        System.out.println(s);
    }

    private void logData(String dataName, String s) {
        System.out.println(String.format("    %s -> %s", dataName, s));
    }
}
