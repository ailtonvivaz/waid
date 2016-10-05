package tech.waid.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tech.waid.app.R;
import tech.waid.app.model.BeaconDistance;
import tech.waid.app.model.Ponto;
import tech.waid.app.trilateration.NonLinearLeastSquaresSolver;
import tech.waid.app.trilateration.TrilaterationFunction;
import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.data.*;

public class MapActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000000000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    ArrayList<BeaconDistance> _listBeacons = new ArrayList<tech.waid.app.model.BeaconDistance>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        CreateBeacons();

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

    }
    //Cria os beacons com suas cordenadas
    protected  void CreateBeacons()
    {
        tech.waid.app.model.BeaconDistance b1 = new tech.waid.app.model.BeaconDistance(24,new Ponto(0, 0, 0), "0C:F3:EE:03:F3:94",0);
        tech.waid.app.model.BeaconDistance b2 = new tech.waid.app.model.BeaconDistance(16,new Ponto(0, 6.6, 0), "0C:F3:EE:03:FB:03",0);
        tech.waid.app.model.BeaconDistance b3 = new tech.waid.app.model.BeaconDistance(15,new Ponto(11, 6.6, 0), "0C:F3:EE:03:F3:8E",0);

        _listBeacons.add(b1);
        _listBeacons.add(b2);
        _listBeacons.add(b3);

       // ListView list = (ListView)findViewById(R.id.listviewCord);

        ArrayList<String> listBe = new ArrayList<>();
        for(BeaconDistance b: _listBeacons)
        {
            listBe.add("Beacon ID: "+ b.getId()+ " | Ponto: "+ String.format( "(%.2f, %.2f, %.2f)", b.getPosicao().getX(),  b.getPosicao().getY(), b.getPosicao().getZ()));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listBe);
        //list.setAdapter(adapter);
    }
    protected double Get2DDistance(double distanceReal, double Height)
    {
        return Math.pow(((distanceReal*distanceReal)-(Height*Height)), 0.5);
    }

    protected void GetPositionUser()
    {

        double[][] positions = new double[][] {
                { _listBeacons.get(0).getPosicao().getX(), _listBeacons.get(0).getPosicao().getY(), _listBeacons.get(0).getPosicao().getZ() },
                { _listBeacons.get(1).getPosicao().getX(), _listBeacons.get(1).getPosicao().getY(), _listBeacons.get(1).getPosicao().getZ() },
                { _listBeacons.get(2).getPosicao().getX(), _listBeacons.get(2).getPosicao().getY(), _listBeacons.get(2).getPosicao().getZ() }
        };
        double[] distance = new double[] {
                _listBeacons.get(0).getDistance(), _listBeacons.get(1).getDistance(), _listBeacons.get(2).getDistance()
        };





        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distance), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        // the answer
        double[] centroid = optimum.getPoint().toArray();
        System.out.println(String.format( "(%.2f, %.2f, %.2f)", centroid[0],  centroid[1], centroid[2]));


        ScatterChart list = (ScatterChart)findViewById(R.id.chart);

        List<Entry> entries = new ArrayList<Entry>();

        // turn your data into Entry objects
        entries.add(new Entry((float)positions[0][0], (float)positions[0][1]));
        entries.add(new Entry((float)positions[2][0], (float)positions[2][1]));
        entries.add(new Entry((float)positions[1][0], (float)positions[1][1]));


        List<Entry> entriesUser = new ArrayList<Entry>();
        entriesUser.add(new Entry((float)centroid[0], (float)centroid[1]));


        ScatterDataSet dataSet = new ScatterDataSet(entries, "Beacons");
        dataSet.setColor(getResources().getColor(R.color.ColorBeacons), 255);
        dataSet.setValueTextColor(getResources().getColor(R.color.ColorBeacons)); // styling, ...

        ScatterDataSet dataSetUser = new ScatterDataSet(entriesUser, "Usuario");
        dataSetUser.setColor(getResources().getColor(R.color.ColorUser), 255);
        dataSetUser.setValueTextColor(getResources().getColor(R.color.ColorUser)); // styling, ...


        ScatterData lineData = new ScatterData(dataSet);//beacons
        lineData.addDataSet(dataSetUser);//user

        list.setData(lineData);
        list.invalidate(); // refresh



       // list.setAdapter(adapter);

    }
    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);

                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }


    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            Log.i("DISPOSITIVO", result.getDevice().toString() + " - distnce: "+calculateAccuracy(-63, result.getRssi()));

            for(BeaconDistance b : _listBeacons)
            {
                if(b.getBluetoothName().equals(result.getDevice().getAddress()))
                {
                    b.setDistance(calculateAccuracy(-63, result.getRssi()));
                    break;
                }
            }
            GetPositionUser();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };
    protected static double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }
}
