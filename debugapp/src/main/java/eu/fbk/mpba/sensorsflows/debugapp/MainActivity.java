package eu.fbk.mpba.sensorsflows.debugapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.UUID;

import eu.fbk.mpba.sensorsflows.AutoLinkMode;
import eu.fbk.mpba.sensorsflows.FlowsMan;
import eu.fbk.mpba.sensorsflows.debugapp.plugins.EXLs3Device;
import eu.fbk.mpba.sensorsflows.debugapp.plugins.SmartphoneDevice;
import eu.fbk.mpba.sensorsflows.debugapp.plugins.outputs.ProtobufferOutput;
import eu.fbk.mpba.sensorsflows.debugapp.plugins.outputs.SQLiteOutput;
import eu.fbk.mpba.sensorsflows.debugapp.util.EXLs3Manager;


public class MainActivity extends Activity {

    FlowsMan<Long, double[]> m = new FlowsMan<>();
    SmartphoneDevice smartphoneDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void onMStart(View v) {
        m.addDevice(smartphoneDevice = new SmartphoneDevice(this, "Smartphone"));

        m.addDevice(new EXLs3Device(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("00:80:e1:b3:4e:a9".toUpperCase()), "EXL"));

//        m.addOutput(new CsvOutput("CSV",
//                Environment.getExternalStorageDirectory().getPath()
//                        + "/eu.fbk.mpba.sensorsflows/"));

        m.addOutput(new SQLiteOutput("DB",
                Environment.getExternalStorageDirectory().getPath()
                        + "/eu.fbk.mpba.sensorsflows/"));

        m.addOutput(new ProtobufferOutput("Protobuf", new File(
                Environment.getExternalStorageDirectory().getPath()
                        + "/eu.fbk.mpba.sensorsflows/"), 1000, UUID.randomUUID().toString()));

        m.setAutoLinkMode(AutoLinkMode.PRODUCT);

        m.start();
    }

    public void onMClose(View v) {
        m.close();
        m = new FlowsMan<>();
    }

    EXLs3Manager s;

    public void onBTSTest(View v) {
        s.connect(BluetoothAdapter.getDefaultAdapter().getRemoteDevice("00:80:e1:b3:4e:a9".toUpperCase()), false);
    }

    public void onWriteTest(View v) {
        s.sendStart();
    }

    public void onStopTest(View v) {
        s.sendStop();
    }

    public void onBTCloseTest(View v) {
        s.stop();
    }

    public void onAddText(View v) {
        smartphoneDevice.addNoteNow(((Button)v).getText().toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }
}