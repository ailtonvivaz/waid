package tech.waid.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tech.waid.app.Services.UserService;
import tech.waid.app.model.BeaconDistance;
import tech.waid.app.model.EventInfo;
import tech.waid.app.model.Ponto;
import tech.waid.app.trilateration.NonLinearLeastSquaresSolver;
import tech.waid.app.trilateration.TrilaterationFunction;

import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.data.*;

import net.servicestack.func.Predicate;

import static net.servicestack.func.Func.filter;

public class MapActivity extends AppCompatActivity implements BeaconConsumer {

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000000000;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    ArrayList<BeaconDistance> _listBeacons = new ArrayList<tech.waid.app.model.BeaconDistance>();
    ArrayList<EventInfo> _listEvents = new ArrayList<EventInfo>();
    protected static final String TAG = "RangingActivity";
    private BeaconManager beaconManager;
    private TextView textcomponent;
    private ScatterChart list;
    private TextView cordPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        CreateBeacons();
        CreateEvents();

        beaconManager = BeaconManager.getInstanceForApplication(this);
        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.bind(this);
        textcomponent = (TextView)findViewById(R.id.textPosition);
        cordPosition = (TextView)findViewById(R.id.cordPosition);
        list = (ScatterChart)findViewById(R.id.chart);
    }
    protected void CreateEvents()
    {
        _listEvents = new ArrayList<>();

        _listEvents.add(new EventInfo("Ponto Informativo","Quadro",1, new Ponto(0,5,0), R.color.ColorEventBLUE));
        _listEvents.add(new EventInfo("Ponto Informativo","Porta de saida",1, new Ponto(3,8,0), R.color.ColorEventBLUE));
        _listEvents.add(new EventInfo("Ponto ALERTA","Cuidado escadas!",1, new Ponto(3,5,0), R.color.ColorEventRED));
        _listEvents.add(new EventInfo("Ponto ALERTA","Cuidado um burraco",1, new Ponto(5,9.0,0), R.color.ColorEventRED));
    }

    //Cria os beacons com suas cordenadas
    protected void CreateBeacons()
    {
        //tech.waid.app.model.BeaconDistance b1 = new tech.waid.app.model.BeaconDistance(24,new Ponto(0, 0, 0), "0C:F3:EE:03:F3:94",0);
        //tech.waid.app.model.BeaconDistance b2 = new tech.waid.app.model.BeaconDistance(16,new Ponto(0, 2.45, 0), "0C:F3:EE:03:FB:03",0);
        //tech.waid.app.model.BeaconDistance b3 = new tech.waid.app.model.BeaconDistance(15,new Ponto(2.88, 2.20, 0), "0C:F3:EE:03:F3:8E",0);

        tech.waid.app.model.BeaconDistance b1 = new tech.waid.app.model.BeaconDistance(24,new Ponto(0, 0, 0), "0C:F3:EE:03:F3:94",0);
        tech.waid.app.model.BeaconDistance b2 = new tech.waid.app.model.BeaconDistance(16,new Ponto(0, 6.6, 0), "0C:F3:EE:03:FB:03",0);
        tech.waid.app.model.BeaconDistance b3 = new tech.waid.app.model.BeaconDistance(15,new Ponto(11, 6.6, 0), "0C:F3:EE:03:F3:8E",0);
        tech.waid.app.model.BeaconDistance b4 = new tech.waid.app.model.BeaconDistance(21,new Ponto(11, 0, 0), "0C:F3:EE:03:FB:0A",0);

        _listBeacons.add(b1);
        _listBeacons.add(b2);
        _listBeacons.add(b3);
        _listBeacons.add(b4);

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

    //POPULA COM A POSICAO DOS BEACONS
        double[][] positions = new double[][] {
                { _listBeacons.get(0).getPosicao().getX(), _listBeacons.get(0).getPosicao().getY(), _listBeacons.get(0).getPosicao().getZ() },
                { _listBeacons.get(1).getPosicao().getX(), _listBeacons.get(1).getPosicao().getY(), _listBeacons.get(1).getPosicao().getZ() },
                { _listBeacons.get(2).getPosicao().getX(), _listBeacons.get(2).getPosicao().getY(), _listBeacons.get(2).getPosicao().getZ() },
                { _listBeacons.get(3).getPosicao().getX(), _listBeacons.get(3).getPosicao().getY(), _listBeacons.get(3).getPosicao().getZ() }
        };
        //POPULA COM A DISTANCIA DO USUARIO AOS BEACONS
        double[] distance = new double[] {
                _listBeacons.get(0).getDistance(), _listBeacons.get(1).getDistance(), _listBeacons.get(2).getDistance(), _listBeacons.get(3).getDistance()
        };




        //ALGORITMO DE TRILATERAÇÃO
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distance), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();

        // PRINTA NA TELA AS COORDENADAS COLETADAS
        double[] centroid = optimum.getPoint().toArray();
        cordPosition.setText(String.format( "(%.2f, %.2f, %.2f)", centroid[0],  centroid[1], centroid[2]));

        //ENVIA PARA O BANCO DE DADOS PARA VISUALIZAÇÃO NO BLENDER
        try {

            UserService.InsertPosition(centroid[0], centroid[1], centroid[2]);//ENVIA PARA O BANCO DE DADOS
        }
        catch (Exception e)
        {

        }


        List<Entry> entries = new ArrayList<Entry>();

        // turn your data into Entry objects
        entries.add(new Entry((float)positions[0][0], (float)positions[0][1]));
        entries.add(new Entry((float)positions[2][0], (float)positions[2][1]));
        entries.add(new Entry((float)positions[3][0], (float)positions[3][1]));
        entries.add(new Entry((float)positions[1][0], (float)positions[1][1]));


        List<Entry> entriesUser = new ArrayList<Entry>();
        entriesUser.add(new Entry((float)centroid[0], (float)centroid[1]));

        ArrayList<Entry> entriesEventInfo = new ArrayList<>();

        ArrayList<EventInfo> _listEventsInformativo = filter(_listEvents, new Predicate<EventInfo>() {
            @Override
            public boolean apply(EventInfo p) {
                return p.getColor() == R.color.ColorEventBLUE;
            }
        });
        //LISTA DE PONTOS INFORMATIVOS
        for (EventInfo event : _listEventsInformativo)
        {
            entriesEventInfo.add(new Entry(((float)event.getPonto().getX()), ((float)event.getPonto().getY()), (float)event.getPonto().getZ()));
        }


        ArrayList<Entry> entriesEventAlert = new ArrayList<>();

        ArrayList<EventInfo> _listEventsAlerta = filter(_listEvents, new Predicate<EventInfo>() {
            @Override
            public boolean apply(EventInfo p) {
                return p.getColor() == R.color.ColorEventRED;
            }
        });
        //LISTA DE PONTOS DE ALERTA
        for (EventInfo event : _listEventsAlerta)
        {
            entriesEventAlert.add(new Entry(((float)event.getPonto().getX()), ((float)event.getPonto().getY()), (float)event.getPonto().getZ()));
        }

        ScatterDataSet dataSet = new ScatterDataSet(entries, "Beacons");
        dataSet.setColor(getResources().getColor(R.color.ColorEventGREEN), 255);
        dataSet.setValueTextColor(getResources().getColor(R.color.ColorEventGREEN)); // styling, ...

        ScatterDataSet dataSetUser = new ScatterDataSet(entriesUser, "Usuario");
        dataSetUser.setColor(getResources().getColor(R.color.ColorUser), 255);
        dataSetUser.setValueTextColor(getResources().getColor(R.color.ColorUser)); // styling, ...
        dataSetUser.setScatterShape(ScatterChart.ScatterShape.TRIANGLE);

        ScatterDataSet dataSetEventInfo = new ScatterDataSet(entriesEventInfo, "Evento Informativo");
        dataSetEventInfo.setColor(getResources().getColor(R.color.ColorEventBLUE), 255);
        dataSetEventInfo.setValueTextColor(getResources().getColor(R.color.ColorEventBLUE)); // styling, ...
        dataSetEventInfo.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

        ScatterDataSet dataSetAlerta = new ScatterDataSet(entriesEventAlert, "Evento Alerta");
        dataSetAlerta.setColor(getResources().getColor(R.color.ColorEventRED), 255);
        dataSetAlerta.setValueTextColor(getResources().getColor(R.color.ColorEventRED)); // styling, ...
        dataSetAlerta.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

        ScatterData lineData = new ScatterData(dataSet);//beacons
        lineData.addDataSet(dataSetUser);//user
        lineData.addDataSet(dataSetEventInfo);//user
        lineData.addDataSet(dataSetAlerta);//user

        list.setData(lineData);
        list.invalidate(); // refresh

        //VERIFICA SE O PONTO COLETADO ESTÁ DENTRO DE UM DOS RAIOS DOS PONTOS DE ALERTA OU INFORMATIVOS
        try {
            //ponto de alerta
            for (EventInfo event : _listEventsAlerta)
            {
                double x = Math.pow(event.getPonto().getX() - centroid[0], 2);
                double y = Math.pow(event.getPonto().getY() - centroid[1], 2);
                double z = Math.pow(event.getPonto().getZ() - centroid[2], 2);

                double raio = Math.pow(x+y+z, 0.5);

                if(raio <= event.getRangeToHit())
                {
                    String msg = "ALERTA: "+event.getDescription();
                    //Está dentro do raio de disparo
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                    MediaPlayer mp = MediaPlayer.create(this, R.raw.alertsound);
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {

                            mp.release();
                        }

                    });
                    mp.start();

                }
            }
            //ponto de informação
            for (EventInfo event : _listEventsInformativo)
            {
                double x = Math.pow(event.getPonto().getX() - centroid[0], 2);
                double y = Math.pow(event.getPonto().getY() - centroid[1], 2);
                double z = Math.pow(event.getPonto().getZ() - centroid[2], 2);

                double raio = Math.pow(x+y+z, 0.5);

                if(raio <= event.getRangeToHit())
                {
                    String msg = "INFO: "+event.getDescription();
                    //Está dentro do raio de disparo
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();

                    MediaPlayer mp = MediaPlayer.create(this, R.raw.infosound);
                    mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                        @Override
                        public void onCompletion(MediaPlayer mp) {

                            mp.release();
                        }

                    });
                    mp.start();
                }
            }
        }
        catch (Exception e)
        {

        }

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
                    try {
                        for (Beacon be : beacons) {

                            for (BeaconDistance b : _listBeacons) {
                                if (b.getBluetoothName().equals(be.getBluetoothAddress())) {
                                    b.setDistance(be.getDistance());
                                    break;
                                }
                            }

                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String textField = "";
                                //print
                                for (BeaconDistance b : _listBeacons) {
                                    textField += (+b.getId() + ": " + String.format("%.2f", b.getDistance()) + " | ");
                                }
                                textcomponent.setText(textField);

                                GetPositionUser();
                            }
                        });
                    }
                    catch (Exception e )
                    {
                        String error = e.getMessage().toString();
                    }
                }
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }
    }


}
