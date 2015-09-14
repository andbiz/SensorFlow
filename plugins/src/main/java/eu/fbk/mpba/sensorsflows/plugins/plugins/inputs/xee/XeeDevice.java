package eu.fbk.mpba.sensorsflows.plugins.plugins.inputs.xee;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.dquid.xee.driver.DQDriver;
import com.dquid.xee.driver.DQDriverEventListener;
import com.dquid.xee.driver.DQSourceType;
import com.dquid.xee.sdk.DQAccelerometerData;
import com.dquid.xee.sdk.DQData;
import com.dquid.xee.sdk.DQGpsData;
import com.dquid.xee.sdk.DQListenerInterface;
import com.dquid.xee.sdk.DQUnitManager;
import com.dquid.xee.sdk.DQUtils;

import java.util.ArrayList;
import java.util.HashMap;

import eu.fbk.mpba.sensorsflows.DevicePlugin;
import eu.fbk.mpba.sensorsflows.SensorComponent;
import eu.fbk.mpba.sensorsflows.base.IMonotonicTimestampReference;

public class XeeDevice implements DevicePlugin<Long, double[]>, DQListenerInterface, DQDriverEventListener, IMonotonicTimestampReference {

    // TODO 8: understand firmware update operation

    private boolean receivingData;
    private BluetoothDevice deviceToConnect;
    private boolean firmwareUPDRequested;
    private boolean connectionRequested;
    private boolean serialNumberRequested;
    private boolean flowing = false;

    protected boolean debug = true;
    protected final String debugTAG = "XeeALE";
    protected DevicePlugin<Long, double[]> parent;

    protected void setDeviceToConnect(BluetoothDevice d) {
        deviceToConnect = d;
    }

    protected void setEnvironment(DQUtils.DQuidEnvs e) {
        if (debug)
            Log.v(debugTAG, "Set environment: " + e);
        DQUtils.setEnvironment(e);
    }

    /**
     * -1. check the bluetooth presence (2.1)
     *  1. set the bt device and environment (?)
     *  2. connection true
     */
    public XeeDevice() {
        if (debug)
            Log.v(debugTAG, "XeeDevice construction");
        DQDriver.INSTANCE.setEventListener(this);
        DQUnitManager.INSTANCE.addListener(this);
        setReceivingData(true);
        if (debug)
            Log.v(debugTAG, "XeeDevice inner construction done");
    }

    public XeeDevice(BluetoothDevice d, DQUtils.DQuidEnvs e) {
        this();
        setDeviceToConnect(d);
        setEnvironment(e);
        connection(true);
    }

    public void inputPluginInitialize(){
        if (debug)
            Log.v(debugTAG, "onResume");

        DQUnitManager.INSTANCE.addListener(this);
        flowing = true;
    }

    public void inputPluginFinalize() {
        if (debug)
            Log.v(debugTAG, "onPause");
        DQDriver.INSTANCE.disableSource(DQSourceType.BLUETOOTH_2_1);
        flowing = false;
    }

    @Override
    public String getName() {
        return deviceToConnect != null ? deviceToConnect.getName() : "UnknownXee";
    }

    @Override
    public Iterable<SensorComponent<Long, double[]>> getSensors() {
        return null; // TODO 6: add sense
    }

    // Device Connected Button
    protected void connection(boolean connect) {
        if (debug)
            Log.v(debugTAG, "connection - connect: " + connect);

        if (connect) {
            // WAS: simulator option
            if (deviceToConnect != null) {
                connectionRequested = true;
                initDriver(false);
            } else {
                throw new NullPointerException("Device to connect to not set.");
            }
        } else {
            disconnectFromBd();
        }
    }

    // Firmware update button
    protected void callFWUpdate(DQUtils.DQuidEnvs e) {
        if (deviceToConnect != null){
            firmwareUPDRequested = true;
            DQDriver.INSTANCE.disableSource(DQSourceType.BLUETOOTH_2_1);
        }
        else {
            broadcastEvent(getMonoUTCNanos(System.nanoTime()), 0, "disconnected");
        }
    }

    private void initDriver(Boolean simulation) {
        if (debug)
            Log.v(debugTAG, "initDriver - simulation: " + simulation);

        if(simulation){
            // DQDriver reads from a can trace (Simulator)
            DQDriver.INSTANCE.enableSource(DQSourceType.SIMULATOR_CAN_TRACE);
        } else {
            // DQDriver reads from bluetooth 2.1 (xee)
            if (deviceToConnect == null)
                Log.wtf(debugTAG, "deviceToConnect not set!");
            DQDriver.INSTANCE.setBtDevice(deviceToConnect);
            DQDriver.INSTANCE.enableSource(DQSourceType.BLUETOOTH_2_1);
        }
    }

    private void setReceivingData(final boolean receiving) {
        if (debug)
            Log.v(debugTAG, "setReceivingData - receiving: " + receiving);

        if (receivingData != receiving) {
            if (receiving) {
                DQUnitManager.INSTANCE.startReceivingCarData();
            } else {
                DQUnitManager.INSTANCE.stopReceivingCarData();
            }
            receivingData = receiving;
        }
    }

    private void disconnectFromBd() {
        if (debug)
            Log.v(debugTAG, "disconnectFromBd");
        DQUnitManager.INSTANCE.disconnect();
    }

    @Override
    public void onConnectionSuccessful() {
        if (debug)
            Log.v(debugTAG, "Connection Successful");
    }

    @Override
    public void onDisconnection() {
        if (debug)
            Log.v(debugTAG, "Disconnected");
        broadcastEvent(getMonoUTCNanos(System.nanoTime()), 0, "disconnected");
    }

    @Override
    public void onError(int arg0, String arg1) {

        if (debug)
            Log.v(debugTAG, "onError: " + arg1);

        if(arg0 == 401){
            Log.e(debugTAG, "ERROR 401!!!! " + arg1);
            DQUnitManager.INSTANCE.disconnect();
        }
        broadcastEvent(getMonoUTCNanos(System.nanoTime()), arg0, arg1);
    }

    @Override
    public void onNewAccelerometerData(DQAccelerometerData arg0) {
        if(debug)
            Log.v(debugTAG, "onNewAccelerometerData - " + arg0.toString());
        // TODO 5: data sensor ACC
    }

    @Override
    public void onNewGpsData(DQGpsData arg0) {
        if(debug)
            Log.v(debugTAG, "onNewGpsData - " + arg0.toString());
        // TODO 5: data sensor GPS
    }

    @Override
    public void onNewData(HashMap<Long, DQData> arg0) {
        //noinspection unused
        HashMap<Long, DQData> dataHashMap = DQUnitManager.INSTANCE.getLastAvailable();
        // TODO 5: data sensor DATA
        // TODO 8: check the difference between arg0 and dataHashMap.
    }

    @Override
    public void onDriverDown(int arg0) {
        if(debug)
            Log.v(debugTAG, "onDriverDown - reason: " + arg0 + " - " + DQDriver.INSTANCE.dqdriverErrorDescriptions.get(arg0));

        // FIXME T: recursive???
        if(firmwareUPDRequested)
            initDriver(false);
    }

    @Override
    public void onDriverReady() {
        if (debug)
            Log.v(debugTAG, "onDriverReady");

        if (firmwareUPDRequested) {
            firmwareUPDRequested = false;
            DQUnitManager.INSTANCE.updateFirmware();
        } else
        if (connectionRequested) {
            connectionRequested = false;
//          if (serialNumberRequested) {
//              DQUnitManager.INSTANCE.getSerialNumber(); // TODO 8 no for now
//          } else {
            if (debug)
                Log.d(debugTAG, "Connecting the INSTANCE");
            DQUnitManager.INSTANCE.connect();
//          }
        } else {
            if (debug)
                Log.i(debugTAG, "...no fw nor conn requested");
            DQUnitManager.INSTANCE.checkFirmwareVersion();
        }
    }

    // unused

    @Override
    public void onDtcCodesAvailable(final ArrayList<String> arg0) {
        if(debug)
            Log.v(debugTAG, "DTC Codes: " + arg0.toString());
    }

    @Override
    public void onDtcNumberAvailable(final int arg0) {
        if (debug)
            Log.v(debugTAG, "DTC Number: " + arg0);
    }

    @Override
    public void onFirmwareUpdateCompleted() {
        if (debug)
            Log.v(debugTAG, "onFirmwareUpdateCompleted");
    }

    @Override
    public void onFirmwareUpdateIncrease(final double arg0) {
        if (debug)
            Log.v(debugTAG, "onFirmwareUpdateIncrease: " + arg0);
    }

    @Override
    public void onFirmwareUpdateNeeded(String versionAvailable) {
        if(debug)
            Log.v(debugTAG, "onFirmwareUpdateNeeded - version " + versionAvailable + " is available");

        firmwareUPDRequested = true;
        DQDriver.INSTANCE.disableSource(DQSourceType.BLUETOOTH_2_1);
    }

    @Override
    public void onFirmwareUpdateNotNeeded() {
        if(debug)
            Log.v(debugTAG, "onFirmwareUpdateNotNeeded");
    }

    @Override
    public void onFirmwareUpdateStarted() {
        if(debug)
            Log.v(debugTAG, "onFirmwareUpdateStarted");
    }

    @Override
    public void onFirmwareVersionObtained(final String arg0) {
        if(debug)
            Log.v(debugTAG, "onFirmwareVersionObtained: " + arg0);

    }

    @Override
    public void onSerialNumberObtained(final String arg0) {
        if(debug)
            Log.v(debugTAG, "onSerialNumberObtained: " + arg0);

        // TODO 8: return it to the user/event
    }

    @Override
    public void onCloseDataSessionAck() {

    }

    @Override
    public void onCloseDataSessionNack() {

    }

    @Override
    public void onOpenDataSessionAck() {

    }

    @Override
    public void onOpenDataSessionNack() {

    }

    @Override
    public void onSettingAck() {

    }

    @Override
    public void onSettingNack() {

    }

    private long bootUTCNanos = System.currentTimeMillis() * 1_000_000L - System.nanoTime();

    @Override
    public long getMonoUTCNanos(long realTimeNanos) {
        return bootUTCNanos + realTimeNanos;
    }

    private void broadcastEvent(long time, int code, String message) {
        if (flowing)
            for (SensorComponent<Long, double[]> i : getSensors())
                i.sensorEvent(time, code, message);
        else
            Log.i(debugTAG, "event: " + time + ", " + code + ", " + message);
    }
}
