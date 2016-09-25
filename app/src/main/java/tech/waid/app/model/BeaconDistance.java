package tech.waid.app.model;


/**
 * Created by felip on 25/09/2016.
 */

public class BeaconDistance extends Beacon{


    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    private double distance;

    public BeaconDistance(int id, Ponto posicao, String bluetoothName, int distance)
    {
        super(id, posicao, bluetoothName);
        this.distance = distance;
    }

}
