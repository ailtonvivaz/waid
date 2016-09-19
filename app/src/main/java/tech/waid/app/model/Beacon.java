package tech.waid.app.model;

/**
 * Created by ailtonvivaz on 19/09/16.
 */

public class Beacon {

    private int id;
    private Ponto posicao;

    public Beacon(int id, Ponto posicao) {
        this.id = id;
        this.posicao = posicao;
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
