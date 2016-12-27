package net.debjeetmaj.androidwifiautologin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * Created by hp 1 on 26-12-2016.
 */

public class WifiConfigDialogFragment extends DialogFragment {
    WifiConfig wifiConfig;
    EditText txtUserName,txtPassword;
    CheckBox chkKeepAlive;
//    View view;
//    Context context;
    final String LOG_TAG = "AndroidAutoLogin";
    public WifiConfigDialogFragment(WifiConfig wifiConfig){
        this.wifiConfig = wifiConfig;
        //this.context = context;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog

        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.wifi_config_fragment,null);
        builder.setView(view)
                // Add action buttons
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // save the network
                        if(!txtUserName.getText().toString().isEmpty() &&  !txtPassword.toString().isEmpty()) {
                            wifiConfig.setUsername(txtUserName.getText().toString());
                            wifiConfig.setPassword(txtPassword.getText().toString());
                            wifiConfig.setKeepAlive(chkKeepAlive.isChecked());
                            SaveWifiConfig();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setTitle(wifiConfig.getSsid())
        ;
        Log.i(LOG_TAG,"Dialog Created.");
        txtUserName = (EditText) view.findViewById(R.id.txtUserName);
        txtPassword = (EditText) view.findViewById(R.id.txtPassword);
        chkKeepAlive = (CheckBox) view.findViewById(R.id.chkKeepAlive);
        LoadWifiConfig();
        txtUserName.setText(wifiConfig.getUsername());
        txtPassword.setText(wifiConfig.getPassword());
        chkKeepAlive.setChecked(wifiConfig.isKeepAlive());
        return builder.create();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    private void LoadWifiConfig(){
        File wifiConfigFile = new File(getActivity().getFilesDir(),this.wifiConfig.getSsid()+".json");
        if(wifiConfigFile.exists()){
            Log.i(LOG_TAG,"Found "+wifiConfigFile.getName()+", attempting read.");
            try {
                String content = new Scanner(wifiConfigFile).useDelimiter("\\Z").next();
//                FileInputStream fis = new FileInputStream(wifiConfigFile);
//                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
//                StringBuffer sb = new StringBuffer();
//                String temp = br.readLine();
//                while(!temp.isEmpty()){
//                    sb.append(temp);
//                    temp = br.readLine();
//                }
                Log.i(LOG_TAG,content);
                JSONObject jsonObject = new JSONObject(content);
                wifiConfig.setUsername(jsonObject.get("username").toString());
                wifiConfig.setPassword(jsonObject.get("password").toString());
                wifiConfig.setKeepAlive((Boolean) jsonObject.get("keepalive"));
            }
            catch (IOException ex){
                Log.e(LOG_TAG,ex.getMessage());
            }
            catch (Exception ex){
                Log.e(LOG_TAG,ex.getMessage());
            }
        }
        else{
            Log.i(LOG_TAG,"File not Found "+wifiConfigFile.getName());
            wifiConfig.setUsername("");
            wifiConfig.setPassword("");
            wifiConfig.setKeepAlive(false);
        }
    }
    private void SaveWifiConfig(){
        File wifiConfigFile = new File(getActivity().getFilesDir(),this.wifiConfig.getSsid()+".json");
        try
        {
            FileOutputStream fos = getActivity().openFileOutput(this.wifiConfig.getSsid()+".json", Context.MODE_PRIVATE);
            fos.write(wifiConfig.toJSONString().getBytes());
            fos.close();
            if(wifiConfigFile.exists())
                Log.i(LOG_TAG,this.wifiConfig.getSsid()+" Config written.");
            else
                Log.i(LOG_TAG,this.wifiConfig.getSsid()+" Config not written.");
        }
        catch (Exception ex){
            Log.e(LOG_TAG,ex.getMessage());
        }

    }
}
