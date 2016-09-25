package tech.waid.app;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import tech.waid.app.model.Beacon;
import tech.waid.app.model.Ponto;
import tech.waid.app.trilateration.NonLinearLeastSquaresSolver;
import tech.waid.app.trilateration.TrilaterationFunction;

import static android.R.attr.id;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Beacon b1 = new Beacon(1, new Ponto(3., 3., 3.));
        Beacon b2 = new Beacon(1, new Ponto(9., 3., 3.));
        Beacon b3 = new Beacon(1, new Ponto(6., 6., 2.));

        final double[][] positions = new double[][] { { b1.getPosicao().getX(), b1.getPosicao().getY(), b1.getPosicao().getZ() }, { b2.getPosicao().getX(), b2.getPosicao().getY(), b2.getPosicao().getZ() }, { b3.getPosicao().getX(), b3.getPosicao().getY(), b3.getPosicao().getZ() } };
        final double[][] distances = new double[][] {
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

        Button but = (Button)findViewById(R.id.but);
        Button search = (Button)findViewById(R.id.searchBeaconsBtn);
        search.setOnClickListener(onClickListener);

        but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for ( double[] d : distances) {

                    NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, d), new LevenbergMarquardtOptimizer());
                    LeastSquaresOptimizer.Optimum optimum = solver.solve();

                    // the answer
                    double[] centroid = optimum.getPoint().toArray();
                    System.out.println(String.format( "(%.2f, %.2f, %.2f)", centroid[0],  centroid[1], centroid[2]));
                }

            }
        });


    }
    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startActivity(new Intent(MainActivity.this, SearchBeaconActivity.class));
        }
    };
}
