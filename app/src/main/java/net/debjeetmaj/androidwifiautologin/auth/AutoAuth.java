package net.debjeetmaj.androidwifiautologin.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public abstract class AutoAuth implements Serializable{
    static final String LOG_TAG = "AutoAuthObj";
    public static final int CONNECTION_TIMEOUT = 15000; // 15 secs
    final static int CONNECTION_CHECK_ATTEMPTS = 10;

    String authUrl;
    String username,password;

    AutoAuth(String authUrl, String username, String password) throws Exception {
        if(authUrl == null || authUrl.isEmpty())
            throw new Exception("Authentication URL cannot be empty or null");

        this.authUrl = authUrl;
        this.username = username;
        this.password = password;
    }

    /* called to authenticate on the network */
    public abstract boolean authenticate();

    /* time to sleep between two calls to authenticate */
    public abstract int sleepTimeout();

    /* Load an AutoAuth object from file */
    public static AutoAuth load(File file){
        FileInputStream fin = null;
        ObjectInputStream oin = null;
        AutoAuth obj = null;

        try {
            fin = new FileInputStream(file);
            oin = new ObjectInputStream(fin);
            obj = (AutoAuth)oin.readObject();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        finally {
            try {
                if (fin != null) fin.close();
                if (oin != null) oin.close();
            } catch (IOException e) {
                    e.printStackTrace();
            }
        }
        return obj;
    }

    /* Save an AutoAuth object to file */
    public static void save(File file, AutoAuth obj){
        FileOutputStream fout = null;
        ObjectOutputStream oos = null;

        try {
            fout = new FileOutputStream(file);
            oos = new ObjectOutputStream(fout);
            oos.writeObject(obj);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fout != null) fout.close();
                if (oos != null) oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
