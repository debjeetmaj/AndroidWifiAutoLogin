package net.debjeetmaj.androidwifiautologin;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView txtView ;
    Button btnActiveWifi,btnStartService,btnStopService;
    ArrayList<String> savedNetworks = null;
    ListView listView;
    final String LOG_TAG = "AndroidAutoLogin";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnActiveWifi = (Button) findViewById(R.id.btnActiveWifi);
        btnStartService =(Button) findViewById(R.id.btnStartService);
        btnStopService = (Button) findViewById(R.id.btnStopService);
        btnActiveWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isWifiConnected()) {
                    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    final String activeWifiName = wifiInfo.getSSID().replace("\"","");
                    btnActiveWifi.setText(activeWifiName);
                    //debug
                    btnActiveWifi.setText(btnActiveWifi.getText()+"\n("+AutoLoginService.getState(getBaseContext())+")");
                    //display fragment
                    WifiConfigDialogFragment wifiConfigDialogFragment = new WifiConfigDialogFragment(new WifiConfig(activeWifiName));
                    wifiConfigDialogFragment.show(getFragmentManager(), "Save network");
                    btnStartService.setVisibility(View.VISIBLE);
                    btnStopService.setVisibility(View.VISIBLE);
                }
                else{
                    btnActiveWifi.setText("Wifi Not Connected !!");
                    //debug
                    btnActiveWifi.setText(btnActiveWifi.getText()+"\n("+AutoLoginService.getState(getBaseContext())+")");
                    btnStartService.setVisibility(View.INVISIBLE);
                    btnStopService.setVisibility(View.INVISIBLE);
                }
            }
        });
        final Intent intent = new Intent(getApplicationContext(), AutoLoginService.class);
        btnStartService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoLoginService.setState(getBaseContext(),LoginState.START);
                startService(intent);
//                btnActiveWifi.setText(btnActiveWifi.getText()+"\n("+AutoLoginService.getState()+")");
            }
        });
        btnStopService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AutoLoginService.setState(getBaseContext(),LoginState.STOPPED);
                stopService(intent);
                // clean up with stopped state
                startService(intent);
//                btnActiveWifi.setText(btnActiveWifi.getText()+"\n("+AutoLoginService.getState()+")");
            }
        });
        listView =(ListView) findViewById(R.id.listView);
        loadSavedNetworks();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        checkWifiConnection();
        loadSavedNetworks();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkWifiConnection();
        loadSavedNetworks();
    }


    private void loadSavedNetworks(){
        String[] ssids = getFilesDir().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });
        for(int i=0;i<ssids.length;i++){
            ssids[i]=ssids[i].substring(0,ssids[i].indexOf('.'));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                            R.layout.wifi_item, R.id.lblSSID, ssids);
        listView.setAdapter(adapter);
    }
    private void checkWifiConnection(){
        try{
            if(isWifiConnected()){
                Toast.makeText(getApplicationContext(), "Wifi is connected", Toast.LENGTH_SHORT);
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                final String activeWifiName = wifiInfo.getSSID().replace("\"","");
                btnActiveWifi.setText(activeWifiName);
                //debug
                btnActiveWifi.setText(btnActiveWifi.getText()+"\n("+AutoLoginService.getState(getBaseContext())+")");

            }
            else{
                btnActiveWifi.setText("Wifi Not Connected !!");
                //debug
                btnActiveWifi.setText(btnActiveWifi.getText()+"\n("+AutoLoginService.getState(getBaseContext())+")");

            }}
        catch (Exception ex){
            txtView.setText(ex.getMessage());
            //Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG);
        }
    }
    private boolean isWifiConnected(){
        try {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getActiveNetworkInfo();

            if (mWifi!=null && mWifi.getType()== ConnectivityManager.TYPE_WIFI && mWifi.isConnected()) {
                return true;
            }
        }catch (Exception ex){}
        return false;
    }
}
