package com.rfduino.examples;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.rfduino.R;
import com.rfduino.core.BluetoothLEStack;

/**
 * ListAllExamples.java
 * <p/>
 * This Activity:
 * 1.  loads a list of all possible example Activities for use with an RFDuino board and displays them in a clickable format
 * 2. Allows the user to select an Activity via a listElementListener.
 * 3. Performs a Bluetooth Low Energy device scan and displays available Bluetooth devices that can be used with the Activity
 * 4. Stores the selected BluetoothDevice as an "Extra" to pass to the new Activity's "onCreate" method when it is initialized.
 *
 * @author adrienne
 *         <p/>
 *         This library is released under the LGPL. A copy of the license should have been distributed with this library/source code,
 *         if not, you can read it here: (https://github.com/abolger/awesomesauce-rfduino/blob/master/LICENSE)
 */

public class MainActivity extends Activity {

    Intent chosenExample;
    BluetoothDevice chosenBluetoothDevice;
    TextView mBluetoothStatusText;
    Boolean turnOnBluetoothWhenOff = false;
    Boolean searchWhenBluetoothTurnedOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mBluetoothStatusText = (TextView) findViewById(R.id.bluetoothStatusText);
        Button connectButton = (Button) findViewById(R.id.connectButton);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartBluetoothAndSearch();
            }
        });

        if (isBluetoothOn()) {
            setBluetoothStatusText("Bluetooth on");
        } else {
            setBluetoothStatusText("Bluetooth off");
        }

        // Register for broadcasts on BluetoothAdapter state change
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);
    }

    private void restartBluetoothAndSearch() {

        if (isBluetoothOn()) {
            turnOnBluetoothWhenOff = true;
            setBluetooth(false);
        } else {
            turnOnBluetoothAndSearch();
        }
    }

    private void turnOnBluetoothAndSearch() {
        if (!isBluetoothOn()) {
            searchWhenBluetoothTurnedOn = true;
            setBluetooth(true);
        } else {
            searchForDevices();
        }
    }

    private void searchForDevices() {
        chosenExample = new Intent(MainActivity.this, Doos.class);

        //Runs code that pops up a second list on the UI screen- this one shows all possible bluetooth devices that we can use in our examples.
        BluetoothLEStack.beginSearchingForBluetoothDevices(MainActivity.this);
        BluetoothLEStack.showFoundBluetoothDevices(MainActivity.this, rfduinoChosenListener);

    }

    private boolean setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
        return true;
    }

    private boolean isBluetoothOn() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter.isEnabled();
    }

    private void setBluetoothStatusText(final String text) {
        mBluetoothStatusText.setText(text);
    }

    @Override
    public void onDestroy() {
        //Runs code that pops up a second list on the UI screen- this one shows all possible bluetooth devices that we can use in our examples.
        BluetoothLEStack.stopSearchingForBluetoothDevices(MainActivity.this);

        this.unregisterReceiver(mReceiver);
        super.onDestroy();
    }


    /**
     * GUI OnClickListener: after a list of possible RFDuinos is displayed, this handler listens for a click in the list and connects
     * to the corresponding radio.
     */
    DialogInterface.OnClickListener rfduinoChosenListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, final int which) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BluetoothLEStack.stopSearchingForBluetoothDevices(MainActivity.this);

                    chosenBluetoothDevice = BluetoothLEStack.discoveredDevices.get(which);
                    chosenExample.putExtra("bluetooth_device", chosenBluetoothDevice);

                    startActivity(chosenExample);
                }
            });
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        setBluetoothStatusText("Bluetooth off");
                        if (turnOnBluetoothWhenOff) {
                            turnOnBluetoothAndSearch();
                            turnOnBluetoothWhenOff = false;
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        setBluetoothStatusText("Turning Bluetooth off...");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        setBluetoothStatusText("Bluetooth on");
                        if (searchWhenBluetoothTurnedOn) {
                            searchForDevices();
                            searchWhenBluetoothTurnedOn = false;
                        }
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        setBluetoothStatusText("Turning Bluetooth on...");
                        break;
                }
            }
        }
    };
}
