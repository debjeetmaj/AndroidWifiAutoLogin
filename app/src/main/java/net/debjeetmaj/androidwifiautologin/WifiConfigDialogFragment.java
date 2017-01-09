package net.debjeetmaj.androidwifiautologin;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import java.io.File;

public class WifiConfigDialogFragment extends DialogFragment {
    WifiConfig wifiConfig;
    EditText txtUserName,txtPassword;
    CheckBox chkKeepAlive;

    final String LOG_TAG = "AndroidAutoLogin";
    public WifiConfigDialogFragment(){
        super();
    }

    @SuppressLint("ValidFragment")
    public WifiConfigDialogFragment(WifiConfig wifiConfig){
        super();
        this.wifiConfig = wifiConfig;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog

        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.wifi_config_fragment,null);
        builder.setView(view);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // save the network
                if (!txtUserName.getText().toString().isEmpty() && !txtPassword.toString().isEmpty()) {
                    wifiConfig.setUsername(txtUserName.getText().toString());
                    wifiConfig.setPassword(txtPassword.getText().toString());
                    wifiConfig.setKeepAlive(chkKeepAlive.isChecked());
                    WifiConfig.saveWifiConfig(wifiConfig,
                            new File(getActivity().getFilesDir(), WifiConfig.getFileName(wifiConfig.getSsid())));
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setTitle(wifiConfig.getSsid());
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
        File wifiConfigFile = new File(getActivity().getFilesDir(), WifiConfig.getFileName(this.wifiConfig.getSsid()));
        if(wifiConfigFile.exists()){
            Log.i(LOG_TAG,"Found "+wifiConfigFile.getName()+", attempting read.");
            try {
                wifiConfig = WifiConfig.loadWifiConfig(wifiConfigFile);
            } catch (Exception ex){
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
}
