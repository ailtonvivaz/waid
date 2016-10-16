package tech.waid.app.model;

/**
 * Created by felip on 15/10/2016.
 */

public class EventInfo {
    private String title;
    private String description;
    private double rangeToHit;
    private Object data;
    private Ponto ponto;
    private int Color;

    public EventInfo(String title, String description, double Range, Ponto ponto, int color)
    {
        this.title = title;
        this.description = description;
        this.rangeToHit = Range;
        this.ponto = ponto;
        this.setColor(color);
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getRangeToHit() {
        return rangeToHit;
    }

    public void setRangetToHit(double rangeToHit) {
        this.rangeToHit = rangeToHit;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
    public void setPonto(Ponto ponto)
    {
        this.ponto = ponto;
    }
    public Ponto getPonto()
    {
        return this.ponto;
    }

    public int getColor() {
        return Color;
    }

    public void setColor(int color) {
        Color = color;
    }
}

