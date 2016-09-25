package tech.waid.app.model;

/**
 * Created by ailtonvivaz on 19/09/16.
 */

public class Beacon {

    protected int id;
    protected Ponto posicao;

    public String getBluetoothName() {
        return bluetoothName;
    }

    public void setBluetoothName(String bluetoothName) {
        this.bluetoothName = bluetoothName;
    }

    protected String bluetoothName;

    public Beacon(int id, Ponto posicao, String bluetoothName) {
        this.id = id;
        this.posicao = posicao;
        this.bluetoothName = bluetoothName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Ponto getPosicao() {
        return posicao;
    }

    public void setPosicao(Ponto posicao) {
        this.posicao = posicao;
    }
}
