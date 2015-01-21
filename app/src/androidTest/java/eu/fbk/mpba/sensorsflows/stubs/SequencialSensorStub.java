package eu.fbk.mpba.sensorsflows.stubs;

import java.util.Arrays;
import java.util.List;

import eu.fbk.mpba.sensorsflows.DeviceImpl;
import eu.fbk.mpba.sensorsflows.SensorImpl;
import eu.fbk.mpba.sensorsflows.base.SensorStatus;

public class SequencialSensorStub extends SensorImpl<Long, float[]> {

    private String _name;
    private volatile boolean _streaming = false;

    public SequencialSensorStub(DeviceImpl<Long, float[]> d) {
        _name = "SequentialSensor";
        setParentDevice(d);
        final SequencialSensorStub t = this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                float[] seq = new float[] { 0, 0, 0 };
                boolean loop = true;
                while (loop) {
                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        loop = false;
                    }
                    if (_streaming) {
                        t.eventValue(new float[] { seq[0], seq[1], seq[2] } );
                        seq[0] += 1;
                        seq[1] += -.3;
                        seq[2] += .001;
                    }
                }
            }
        }, "PushThread" + toString()).start();
    }

    private void eventValue(float[] v) {
        sensorValue(System.currentTimeMillis(), v);
    }

    @Override
    public void switchOnAsync() {
        if (getState() == SensorStatus.OFF) {
            _streaming = true;
            changeStatus(SensorStatus.ON);
            sensorEvent(System.currentTimeMillis(), 0, _name + " switched on");
        }
    }

    @Override
    public void switchOffAsync() {
        if (getState() == SensorStatus.ON) {
            _streaming = false;
            changeStatus(SensorStatus.OFF);
            sensorEvent(System.currentTimeMillis(), 0, _name + " switched off");
        }
    }

    @Override
    public String toString() {
        return getParentDevice().toString() + "/" + _name;
    }

    @Override
    public List<Object> getValuesDescriptors() {
        return Arrays.asList((Object)"SeqX", "SeqY", "SeqZ");
    }
}
