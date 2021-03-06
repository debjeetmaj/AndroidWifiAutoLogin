package net.debjeetmaj.androidwifiautologin;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    TextView txtView ;
    Button btnActiveWifi,btnStartService,btnStopService;
    ListView listView;

    final static String LOG_TAG = "AndroidAutoLogin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadSavedNetworks(){
        String[] ssids = WifiConfig.getStoredSSIDs(getFilesDir());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.wifi_item, R.id.lblSSID, ssids);
        listView.setAdapter(adapter);
    }
    private void checkWifiConnection(){
        try{
            if(isWifiConnected()){
                Toast.makeText(getApplicationContext(), "Wifi is connected", Toast.LENGTH_SHORT).show();
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
        }catch (Exception ignored){}
        return false;
    }
}
