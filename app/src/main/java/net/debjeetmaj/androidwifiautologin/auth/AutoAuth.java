package net.debjeetmaj.androidwifiautologin.auth;

/**
 * Created by hp 1 on 29-12-2016.
 */

public abstract class AutoAuth {
    public static final String LOG_TAG = "AutoAuthObj";
    protected String authUrl;
    protected String username,password;

    public AutoAuth(String authUrl, String username, String password)throws Exception {
        if(authUrl==null && authUrl.isEmpty())
            throw new Exception("Authentication url cannot be empty or null");
        this.authUrl = authUrl;
        this.username = username;
        this.password = password;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    /*
     * called to authenticate on the network
     */
    public abstract void authenticate();
    /*
    * called after authenticate, to keep the connection authenticated
    * */
    public abstract void keepAlive();
}
