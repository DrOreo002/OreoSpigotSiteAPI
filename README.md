# OreoSpigotSiteAPI
An API for accessing spigot site, with CloudFlare bypasser

# Building
This project uses Gralde. You can directly build it with `clean build shadowJar publishToMavenLocal test` command. This will also test any available test cases. You can disable testing by removing the `test` argument. But I'd recommend to use it because it will make sure everything is okay

# Usage
Simply initialize `SpigotSite` first using `SpigotSite.init(YourSpigotCredentialsFile);`
* Credentials file should be `spigotUserName,spigotPassword,spigotToTP` can be named whatever you like, `txt` should just do the job
