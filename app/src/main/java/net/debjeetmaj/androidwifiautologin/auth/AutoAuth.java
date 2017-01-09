package net.debjeetmaj.androidwifiautologin.auth;

public abstract class AutoAuth {
    static final String LOG_TAG = "AutoAuthObj";
    public static final int CONNECTION_TIMEOUT = 100000; // msecs

    String authUrl;
    String username,password;

    AutoAuth(String authUrl, String username, String password)throws Exception {
        if(authUrl == null || authUrl.isEmpty())
            throw new Exception("Authentication URL cannot be empty or null");

        this.authUrl = authUrl;
        this.username = username;
        this.password = password;
    }

    /*
     * called to authenticate on the network
     */
    public abstract boolean authenticate();

    /*
     * time to sleep between two calls to authenticate
     */
    public abstract int sleepTimeout();
}
