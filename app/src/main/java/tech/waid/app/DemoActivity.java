package tech.waid.app;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;

import net.servicestack.func.Predicate;

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
import java.util.concurrent.TimeUnit;

import tech.waid.app.Services.UserService;
import tech.waid.app.model.BeaconDistance;
import tech.waid.app.model.EventInfo;
import tech.waid.app.model.Ponto;
import tech.waid.app.trilateration.NonLinearLeastSquaresSolver;
import tech.waid.app.trilateration.TrilaterationFunction;

import static net.servicestack.func.Func.filter;


public class DemoActivity extends AppCompatActivity  implements BeaconConsumer {


    ArrayList<BeaconDistance> _listBeacons = new ArrayList<BeaconDistance>();
    ArrayList<EventInfo> _listEvents = new ArrayList<EventInfo>();

    protected static final String TAG = "RangingActivity";
    private TextView textcomponent;
    private ScatterChart list;
    private TextView cordPosition;
    private BeaconManager beaconManager;
    double[][] cords;
    int indexPos = 0;
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

        cords = GetCoordinatesDEMO();


    }
    protected void CreateEvents()
    {
        _listEvents = new ArrayList<>();

        _listEvents.add(new EventInfo("Ponto Informativo","Quadro",1, new Ponto(0,0,1), R.color.ColorEventBLUE));
        _listEvents.add(new EventInfo("Ponto Informativo","Porta de saida",1, new Ponto(5.2,0.3,1), R.color.ColorEventBLUE));
        _listEvents.add(new EventInfo("Ponto ALERTA","Cuidado escadas!",1, new Ponto(10.5, 0.5 ,1), R.color.ColorEventRED));
        _listEvents.add(new EventInfo("Ponto ALERTA","Cuidado um burraco",1, new Ponto(0.5,3.0,1), R.color.ColorEventRED));
    }
    public double[] GetCoordinatesByDistances(double[][] positions, double[] distance)
    {
        //ALGORITMO DE TRILATERAÇÃO
        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distance), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();
        double[] centroid = optimum.getPoint().toArray();

        return centroid;
    }
    public ScatterDataSet GetScatterDataSetBeaconsScreen()
    {
        CreateBeacons();

        //POPULA COM A POSICAO DOS BEACONS
        double[][] positions = new double[][]{
                {_listBeacons.get(0).getPosicao().getX(), _listBeacons.get(0).getPosicao().getY(), _listBeacons.get(0).getPosicao().getZ()},
                {_listBeacons.get(1).getPosicao().getX(), _listBeacons.get(1).getPosicao().getY(), _listBeacons.get(1).getPosicao().getZ()},
                {_listBeacons.get(2).getPosicao().getX(), _listBeacons.get(2).getPosicao().getY(), _listBeacons.get(2).getPosicao().getZ()}
                //{ _listBeacons.get(3).getPosicao().getX(), _listBeacons.get(3).getPosicao().getY(), _listBeacons.get(3).getPosicao().getZ() }
        };

        List<Entry> entries = new ArrayList<Entry>();

        // turn your data into Entry objects
        entries.add(new Entry((float) positions[0][0], (float) positions[0][1]));
        entries.add(new Entry((float) positions[2][0], (float) positions[2][1]));
        //entries.add(new Entry((float)positions[3][0], (float)positions[3][1]));
        entries.add(new Entry((float) positions[1][0], (float) positions[1][1]));

        ScatterDataSet dataSet = new ScatterDataSet(entries, "Beacons");
        dataSet.setColor(getResources().getColor(R.color.ColorEventGREEN), 255);
        dataSet.setValueTextColor(getResources().getColor(R.color.ColorEventGREEN)); // styling, ...

        return dataSet;
    }
    public ScatterDataSet GetScatterDataInformativos()
    {
        ArrayList<Entry> entriesEventInfo = new ArrayList<>();

        ArrayList<EventInfo> _listEventsInformativo = filter(_listEvents, new Predicate<EventInfo>() {
            @Override
            public boolean apply(EventInfo p) {
                return p.getColor() == R.color.ColorEventBLUE;
            }
        });
        //LISTA DE PONTOS INFORMATIVOS
        for (EventInfo event : _listEventsInformativo) {
            entriesEventInfo.add(new Entry(((float) event.getPonto().getX()), ((float) event.getPonto().getY()), (float) event.getPonto().getZ()));
        }

        ScatterDataSet dataSetEventInfo = new ScatterDataSet(entriesEventInfo, "Evento Informativo");
        dataSetEventInfo.setColor(getResources().getColor(R.color.ColorEventBLUE), 255);
        dataSetEventInfo.setValueTextColor(getResources().getColor(R.color.ColorEventBLUE)); // styling, ...
        dataSetEventInfo.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

        return dataSetEventInfo;
    }
    public ScatterDataSet GetScatterDataAlerta()
    {
        ArrayList<Entry> entriesEventAlert = new ArrayList<>();

        ArrayList<EventInfo> _listEventsAlerta = filter(_listEvents, new Predicate<EventInfo>() {
            @Override
            public boolean apply(EventInfo p) {
                return p.getColor() == R.color.ColorEventRED;
            }
        });

        //LISTA DE PONTOS DE ALERTA
        for (EventInfo event : _listEventsAlerta) {
            entriesEventAlert.add(new Entry(((float) event.getPonto().getX()), ((float) event.getPonto().getY()), (float) event.getPonto().getZ()));
        }

        ScatterDataSet dataSetAlerta = new ScatterDataSet(entriesEventAlert, "Evento Alerta");
        dataSetAlerta.setColor(getResources().getColor(R.color.ColorEventRED), 255);
        dataSetAlerta.setValueTextColor(getResources().getColor(R.color.ColorEventRED)); // styling, ...
        dataSetAlerta.setScatterShape(ScatterChart.ScatterShape.CIRCLE);

        return dataSetAlerta;
    }
    public void VerifyAlerts(double[] centroid)
    {
        ArrayList<EventInfo> _listEventsAlerta = filter(_listEvents, new Predicate<EventInfo>() {
            @Override
            public boolean apply(EventInfo p) {
                return p.getColor() == R.color.ColorEventRED;
            }
        });
        //ponto de alerta
        for (EventInfo event : _listEventsAlerta) {
            double x = Math.pow(event.getPonto().getX() - centroid[0], 2);
            double y = Math.pow(event.getPonto().getY() - centroid[1], 2);
            double z = Math.pow(event.getPonto().getZ() - centroid[2], 2);

            double raio = Math.pow(x + y + z, 0.5);

            if (raio <= event.getRangeToHit()) {
                String msg = "ALERTA: " + event.getDescription();
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
    }
    public void VerifyInfo(double[] centroid)
    {
        ArrayList<EventInfo> _listEventsInformativo = filter(_listEvents, new Predicate<EventInfo>() {
            @Override
            public boolean apply(EventInfo p) {
                return p.getColor() == R.color.ColorEventBLUE;
            }
        });

        //ponto de informação
        for (EventInfo event : _listEventsInformativo) {
            double x = Math.pow(event.getPonto().getX() - centroid[0], 2);
            double y = Math.pow(event.getPonto().getY() - centroid[1], 2);
            double z = Math.pow(event.getPonto().getZ() - centroid[2], 2);

            double raio = Math.pow(x + y + z, 0.5);

            if (raio <= event.getRangeToHit()) {
                String msg = "INFO: " + event.getDescription();
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



    //Cria os beacons com suas cordenadas
    protected void CreateBeacons()
    {
        //tech.waid.app.model.BeaconDistance b1 = new tech.waid.app.model.BeaconDistance(24,new Ponto(0, 0, 0), "0C:F3:EE:03:F3:94",0);
        //tech.waid.app.model.BeaconDistance b2 = new tech.waid.app.model.BeaconDistance(16,new Ponto(0, 2.45, 0), "0C:F3:EE:03:FB:03",0);
        //tech.waid.app.model.BeaconDistance b3 = new tech.waid.app.model.BeaconDistance(15,new Ponto(2.88, 2.20, 0), "0C:F3:EE:03:F3:8E",0);

        BeaconDistance b1 = new BeaconDistance(24,new Ponto(0, 0, 0.1), "0C:F3:EE:03:F3:94",0);
        BeaconDistance b2 = new BeaconDistance(16,new Ponto(0, 6.6, 0.02), "0C:F3:EE:03:FB:03",0);
        BeaconDistance b3 = new BeaconDistance(15,new Ponto(11, 6.6, 0.03), "0C:F3:EE:03:F3:8E",0);
        //tech.waid.app.model.BeaconDistance b4 = new tech.waid.app.model.BeaconDistance(21,new Ponto(11, 0, 0), "0C:F3:EE:03:FB:0A",0);

        _listBeacons.add(b1);
        _listBeacons.add(b2);
        _listBeacons.add(b3);
        //_listBeacons.add(b4);

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
    protected void Print(double[] centroid)
    {
        cordPosition.setText(String.format("(%.2f, %.2f, %.2f)", centroid[0], centroid[1], centroid[2]));

        //ENVIA PARA O BANCO DE DADOS PARA VISUALIZAÇÃO NO BLENDER
        try {

            UserService.InsertPosition(centroid[0], centroid[1], centroid[2]);//ENVIA PARA O BANCO DE DADOS
        }
        catch (Exception e) {

        }

        VerifyAlerts(centroid);
        VerifyInfo(centroid);

        List<Entry> entriesUser = new ArrayList<Entry>();
        entriesUser.add(new Entry((float) centroid[0], (float) centroid[1]));

        ScatterDataSet dataSetUser = new ScatterDataSet(entriesUser, "Usuario");
        dataSetUser.setColor(getResources().getColor(R.color.ColorUser), 255);
        dataSetUser.setValueTextColor(getResources().getColor(R.color.ColorUser)); // styling, ...
        dataSetUser.setScatterShape(ScatterChart.ScatterShape.TRIANGLE);

        ScatterDataSet dataSetEventInfo = GetScatterDataInformativos();
        ScatterDataSet dataSetAlerta = GetScatterDataAlerta();
        ScatterDataSet dataSet = GetScatterDataSetBeaconsScreen();

        ScatterData lineData = new ScatterData(dataSet);//beacons
        lineData.addDataSet(dataSetUser);//user
        lineData.addDataSet(dataSetEventInfo);//user
        lineData.addDataSet(dataSetAlerta);//user

        list.setData(lineData);
        list.invalidate(); // refresh

    }
    public void NextDEMOCoordinates()
    {
            try {
                if(cords.length != indexPos-2)
                    indexPos++;
                else
                    indexPos = 0;

                Print(cords[indexPos]);
                TimeUnit.SECONDS.sleep(1);


            }catch (InterruptedException e)
            {

            }
        catch (Exception e)
        {
            indexPos = 0;
        }
    }
    public void ExecuteDEMOCoordinates()
    {
        double[][] cords = GetCoordinatesDEMO();

        for(int i = 0; i < cords.length; i++)
        {
            try {
                Print(cords[i]);
                TimeUnit.SECONDS.sleep(1);

            }catch (InterruptedException e)
            {

            }
        }
    }
    public void ExecuteDEMODistances()
    {
        double[][] distant = GetDistancesDEMO();
        for(int i =0;i < distant.length; i++)
        {
            CreateBeacons();
            //POPULA COM A POSICAO DOS BEACONS
            double[][] positions = new double[][]{
                    {_listBeacons.get(0).getPosicao().getX(), _listBeacons.get(0).getPosicao().getY(), _listBeacons.get(0).getPosicao().getZ()},
                    {_listBeacons.get(1).getPosicao().getX(), _listBeacons.get(1).getPosicao().getY(), _listBeacons.get(1).getPosicao().getZ()},
                    {_listBeacons.get(2).getPosicao().getX(), _listBeacons.get(2).getPosicao().getY(), _listBeacons.get(2).getPosicao().getZ()}
                    //{ _listBeacons.get(3).getPosicao().getX(), _listBeacons.get(3).getPosicao().getY(), _listBeacons.get(3).getPosicao().getZ() }
            };

            double[] cord = GetCoordinatesByDistances(positions, distant[i]);
            Print(cord);
        }
    }

    public double[][] GetCoordinatesDEMO()
    {
        final double[][] cord = new double[][]{
                { 0.5 , 0.0, 1.016 },
                { 1.0 , 0.8, 0.941 },
                { 2.0 , 0.5, 1.058 },
                { 3.0 , 0.6, 0.941 },
                { 4.0 , 0.5, 1.055 },
                { 5.0 , 0.5, 1.011 },
                { 6.0 , 0.7, 1.064 },
                { 7.0 , 0.7, 0.971 },
                { 8.0 , 0.5, 0.952 },
                { 9.0 , 0.6, 1.075 },
                { 10.0, 0.6, 1.06 },
                { 10.5, 0.8, 1.01 },
                { 10.6, 1.5, 1.051 },
                { 10.4, 2.5, 0.977 },
                { 10.4, 3.5, 0.943 },
                { 10.5, 4.5, 0.955 },
                { 10.7, 5.5, 0.944 },
                { 10.7, 6.3, 0.927 },
                { 10.3, 6.0, 0.95 },
                { 10.3, 5.0, 1.082 },
                { 10.4, 4.0, 0.971 },
                { 10.6, 3.0, 0.912 },
                { 10.6, 2.0, 1.083 },
                { 10.6, 1.0, 0.962 },
                { 9.4, 0.4, 0.956 },
                { 8.5, 0.5, 1.08 },
                { 7.5, 0.6, 1.077 },
                { 7.1, 0.9, 1.011 },
                { 7.0, 1.4, 1.042 },
                { 6.9, 2.4, 1.042 },
                { 6.6, 3.0, 1.003 },
                { 7.0, 3.2, 1.002 },
                { 7.0, 2.8, 1.043 },
                { 6.8, 1.8, 1.046 },
                { 6.6, 1.0, 0.942 },
                { 5.8, 0.7, 0.971 },
                { 4.8, 0.6, 0.962 },
                { 3.8, 0.6, 0.979 },
                { 2.8, 0.5, 1.015 },
                { 2.8, 0.5, 1.04 },
                { 1.8, 0.7, 0.973 },
                { 1.3, 0.9, 0.901 },
                { 0.9, 1.6, 0.953 },
                { 0.6, 2.2, 1.045 },
                { 0.5, 3.0, 0.936 },
                { 0.6, 3.8, 0.989 },
                { 1.2, 4.4, 0.983 },
                { 1.5, 4.0, 0.951 },
                { 1.5, 3.6, 1.089 },
                { 1.6, 2.6, 0.94 },
                { 1.5, 1.6, 1.042 },
                { 1.2, 0.6, 1.0 },
                { 0.7, 0.0, 0.938 }
        };

        return cord;
    }
    public double[][] GetDistancesDEMO()
    {
        double[][] distan = new double[][] {
                { 8.70724458873 ,  9.64423727574 ,  5.45751115522 },
                { 8.52622136138 ,  9.13886269716 ,  5.66008101014 },
                { 9.13866411649 ,  8.3549940405 ,  5.3087391214 },
                { 8.98570969 ,  7.00479278373 ,  4.96580011196 },
                { 9.05297303958 ,  6.47421175691 ,  5.05814839811 },
                { 8.49191986377 ,  5.62796042246 ,  4.77682108876 },
                { 7.88917217907 ,  5.21118811272 ,  5.13252377939 },
                { 7.62723651903 ,  5.08536343087 ,  4.95058484726 },
                { 7.23910174908 ,  4.65188057523 ,  5.10921278309 },
                { 6.38458556784 ,  4.77825053709 ,  5.33517004222 },
                { 6.18156817496 ,  4.91512624708 ,  5.27576026859 },
                { 5.61913041872 ,  5.47542875359 ,  5.61605751513 },
                { 5.26980275293 ,  6.01265946708 ,  5.74182690022 },
                { 4.994626837 ,  6.48563128835 ,  5.47079735963 },
                { 4.77146607718 ,  7.11386318555 ,  5.51863028878 },
                { 4.52154557758 ,  7.06555136224 ,  5.56633660542 },
                { 4.45500060851 ,  7.53886797192 ,  5.31779690286 },
                { 4.31676807272 ,  8.05803638466 ,  5.42326765479 },
                { 4.69669358371 ,  8.08788187784 ,  4.98191482146 },
                { 4.70916347409 ,  7.70886280452 ,  4.9386555472 },
                { 4.93024913275 ,  7.95289667456 ,  4.58277794057 },
                { 5.12700447861 ,  8.04080059454 ,  4.2715437376 },
                { 5.82888788492 ,  7.63200179051 ,  3.91361363609 },
                { 5.50392097017 ,  7.30191456889 ,  3.63806890997 },
                { 6.20068641312 ,  7.35930177681 ,  3.40496472787 },
                { 6.26186725568 ,  6.86288588845 ,  3.32468804718 },
                { 5.82938973835 ,  6.26165723058 ,  3.00418124367 },
                { 6.00333998584 ,  5.91008403794 ,  3.03638858844 },
                { 5.88433552961 ,  5.79681006533 ,  2.88206099583 },
                { 6.06804717382 ,  5.17796138621 ,  2.76745133452 },
                { 5.95799169538 ,  5.09007321906 ,  2.85524528216 },
                { 5.39097104245 ,  5.00311748712 ,  3.01425775659 },
                { 5.24155215515 ,  4.64323198371 ,  3.20424819758 },
                { 5.08321744969 ,  4.9914434851 ,  3.11109277211 },
                { 4.70692125393 ,  4.62534778858 ,  3.18284413023 },
                { 4.43175688085 ,  5.01824166028 ,  3.40755327135 },
                { 4.15895923315 ,  5.29386220809 ,  3.4981888094 },
                { 4.01729664639 ,  5.0949415051 ,  3.60691482948 },
                { 4.00029964992 ,  5.7696092277 ,  3.35843962256 },
                { 3.66238517453 ,  5.97234405622 ,  3.60514226286 },
                { 3.77560669724 ,  5.92001978582 ,  3.44855903604 },
                { 3.61370028071 ,  5.94525387368 ,  3.56191867632 },
                { 3.93056163662 ,  5.97230629483 ,  3.39353837704 },
                { 4.06551649202 ,  6.37540813884 ,  3.28243750418 },
                { 4.00753148963 ,  6.10020758807 ,  2.98395138107 },
                { 4.04689200227 ,  6.28508552275 ,  3.03980665939 },
                { 4.24860549652 ,  6.4332761883 ,  2.73720455114 },
                { 4.31876698794 ,  6.50424043515 ,  2.71744787635 },
                { 4.50817434957 ,  5.8920194061 ,  2.42048282103 },
                { 4.75729872091 ,  6.05081460045 ,  2.20761836216 },
                { 4.79780890347 ,  5.76426338988 ,  2.09482750714 },
                { 4.61875097255 ,  5.49683005449 ,  2.02389816086 },
                { 4.73321721572 ,  5.52278380698 ,  1.96424406556 },
                { 4.6662571853 ,  5.20762633066 ,  1.87886606513 },
                { 4.87869593573 ,  4.6137404897 ,  1.93907310492 },
                { 4.70085243315 ,  4.73219532622 ,  1.9196988411 },
                { 4.58013039201 ,  4.57383963569 ,  2.00709339423 },
                { 4.36360075707 ,  4.37324991675 ,  2.0834027637 },
                { 4.09495593346 ,  4.24384276841 ,  2.30881275849 },
                { 3.85500938747 ,  4.18205221432 ,  2.57246297637 },
                { 3.6958573064 ,  4.32389785854 ,  2.69345062457 },
                { 3.64867456034 ,  4.43690190992 ,  2.87670227559 },
                { 3.12093811498 ,  5.0023824673 ,  2.86508835273 },
                { 3.06392532985 ,  4.83554389298 ,  3.09308349698 },
                { 2.74572059892 ,  5.51142688947 ,  3.20394598446 },
                { 2.70493004205 ,  5.52412250972 ,  3.10897465735 },
                { 2.73480973682 ,  5.78950277171 ,  3.22715909907 },
                { 2.87048262508 ,  6.39224141178 ,  3.25447350975 },
                { 2.95664384493 ,  6.193742865 ,  2.94260793283 },
                { 3.13179703939 ,  6.13190076752 ,  2.95627804953 },
                { 3.40803697385 ,  6.81372376334 ,  2.67129719985 },
                { 3.89281977154 ,  6.78977757632 ,  2.44023846755 },
                { 4.17865384325 ,  6.71545285673 ,  2.33610935012 },
                { 4.64732816849 ,  6.4408779193 ,  2.03957262943 },
                { 4.84770687821 ,  6.34633039204 ,  1.71998288405 },
                { 5.32396965933 ,  5.67137943944 ,  1.49076511277 },
                { 5.03401426814 ,  5.3959849456 ,  1.30774774794 },
                { 5.60781070464 ,  5.06528926891 ,  1.27051417252 },
                { 5.65674335149 ,  4.1655901355 ,  1.23483029524 },
                { 5.22200960435 ,  3.92691584391 ,  1.5691797019 },
                { 5.25134303889 ,  3.39462507038 ,  1.81284762016 },
                { 4.92345826361 ,  2.87819904583 ,  2.18481822341 },
                { 5.1281422304 ,  2.43022916875 ,  2.6678861526 },
                { 4.62822424904 ,  2.36846912498 ,  2.94800638281 },
                { 4.15713669415 ,  2.81787799854 ,  3.34482440775 },
                { 3.58659190868 ,  3.28370038477 ,  3.79385942455 },
                { 3.16713010973 ,  4.04821064567 ,  4.3350706393 },
                { 2.6988928807 ,  4.66134784599 ,  4.56988938549 },
                { 2.08228206096 ,  5.6357289082 ,  4.92800058644 },
                { 1.7495476477 ,  6.22108560441 ,  4.77824120896 },
                { 1.57875636683 ,  6.79444200782 ,  5.27975355267 },
                { 2.04166972812 ,  7.16671394265 ,  5.2492337473 },
                { 2.51173607087 ,  8.19940790056 ,  5.07111209537 },
                { 3.21655347258 ,  8.46968990056 ,  5.18022010883 },
                { 3.97174340113 ,  8.33542550118 ,  5.25960173213 },
                { 4.99123522507 ,  9.08867152116 ,  4.76536984397 },
                { 5.60278145453 ,  8.54507308684 ,  4.86741114243 },
                { 6.17129610134 ,  9.14581490708 ,  4.39194873694 },
                { 6.51082653138 ,  8.6461338929 ,  4.21457738511 },
                { 7.51115460643 ,  7.71921955372 ,  4.10411456833 }
        };
        return distan;
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
     super.onDestroy();
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                NextDEMOCoordinates();
                            }
                        });
                    }
                    catch (Exception e )
                    {
                        String error = e.getMessage().toString();
                    }

            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {    }

    }
}
