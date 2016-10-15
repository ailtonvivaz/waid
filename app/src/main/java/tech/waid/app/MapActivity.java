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
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import tech.waid.app.R;
import tech.waid.app.model.BeaconDistance;
import tech.waid.app.model.Ponto;
import tech.waid.app.trilateration.NonLinearLeastSquaresSolver;
import tech.waid.app.trilateration.TrilaterationFunction;
import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.data.*;

public class MapActivity extends AppCompatActivity implements BeaconConsumer {

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000000000;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    ArrayList<BeaconDistance> _listBeacons = new ArrayList<tech.waid.app.model.BeaconDistance>();
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        CreateBeacons();

        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
        TextView textcomponent = (TextView)findViewById(R.id.textPosition);
        textcomponent.setText("Teste");
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
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {

                    for(Beacon be: beacons) {

                        for (BeaconDistance b : _listBeacons) {
                            if (b.getBluetoothName().equals(be.getBluetoothAddress())) {
                                b.setDistance(be.getDistance());
                                break;
                            }
                        }

                    }
                    String textField = "";
                    TextView textcomponent = (TextView)findViewById(R.id.textPosition);
                    //print
                    for(BeaconDistance b : _listBeacons)
                    {
                        textField += ( +b.getId() + ": "+ String.format("%.2f",b.getDistance()) + " | ");
                    }
                    textcomponent.setText(textField);

                    GetPositionUser();
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }


}
