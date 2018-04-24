package software.embedded.cepe6.smartpot;

import android.app.IntentService;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;
import java.util.UUID;

public class PotControl extends AppCompatActivity {

    ToggleButton autoWateringBtn;
    Button setThresholdBtn;
    Button waterBtn;
    Button disconnectBtn;

    TextView soilTemperatureText;
    TextView soilMoistureText;
    TextView airTemperatureText;
    TextView airHumidityText;
    TextView thresholdText;

    EditText waterTime;
    EditText newThresholdText;

    String address = null;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pot_control);

        //receive the address of the bluetooth device
        Intent newint = getIntent();
        address = newint.getStringExtra("EXTRA_ADDRESS");

        BluetoothConnect bc = new BluetoothConnect();
        bc.execute();

        setContentView(R.layout.activity_pot_control);
        autoWateringBtn = findViewById(R.id.autoWateringToggle);
        setThresholdBtn = findViewById(R.id.setThresholdBtn);
        waterBtn = findViewById(R.id.waterBtn);
        disconnectBtn = findViewById(R.id.disconnectBtn);

        soilTemperatureText = findViewById(R.id.sTemp);
        soilMoistureText = findViewById(R.id.sMois);
        airTemperatureText = findViewById(R.id.aTemp);
        airHumidityText = findViewById(R.id.aHum);
        thresholdText = findViewById(R.id.thresholdText);

        newThresholdText = findViewById(R.id.newThreshold);
        waterTime = findViewById(R.id.wateringTime);

        startService(new Intent(this, Listener.class));

        autoWateringBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    try {
                        btSocket.getOutputStream().write("an".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        btSocket.getOutputStream().write("af".getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        setThresholdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    btSocket.getOutputStream().write(("t" + newThresholdText.getText() + "e").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        waterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    btSocket.getOutputStream().write(("w" + waterTime.getText()).getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        disconnectBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (btSocket!=null) //If the btSocket is busy
                {
                    try
                    {
                        btSocket.close(); //close connection
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                finish(); //return to the first layout
            }
        });
    }


    public class BluetoothConnect extends AsyncTask<Void, Void, Void> {
        private ProgressDialog progressDialog;
        private boolean ConnectSuccess = true;

        @Override
        protected Void doInBackground(Void... voids) {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(PotControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progressDialog.dismiss();
        }

        private void msg(String s)
        {
            Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
        }
    }

    public class Listener extends IntentService {

        public Listener(String name) {
            super(name);
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            Log.d("STATE","HERE?");
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(btSocket.getInputStream()));
                while(true) {
                    String line;
                    line = br.readLine();
                    Log.d("INFO", line);
                    switch (line) {
                        case "st": {
                            String value = Integer.parseInt(br.readLine()) + "C";
                            soilTemperatureText.setText(value);
                            break;
                        }
                        case "sm": {
                            String value = Integer.parseInt(br.readLine()) + "%";
                            soilMoistureText.setText(value);
                            break;
                        }
                        case "at": {
                            String value = Integer.parseInt(br.readLine()) + "C";
                            airTemperatureText.setText(value);
                            break;
                        }
                        case "ah": {
                            String value = Integer.parseInt(br.readLine()) + "%";
                            airHumidityText.setText(value);
                            break;
                        }
                        case "tn": {
                            String value = Integer.parseInt(br.readLine()) + "%";
                            thresholdText.setText(value);
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
