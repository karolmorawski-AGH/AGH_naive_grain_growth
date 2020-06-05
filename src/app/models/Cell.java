package app.models;


import javafx.scene.paint.Color;

public class Cell {
    private Color color;
    // Center of gravity
    private float xGravCent;
    private float yGravCent;
    private boolean isSet = false;
    private int id;
    private short energy;

    public Cell(Color col, float x, float y, int idd) {
        this.color = col;
        this.xGravCent = x;
        this.yGravCent = y;
        this.id = idd;
        this.isSet = true;
    }

    public void setupCell(int idd, Color col) {
        this.id = idd;
        this.color = col;
        this.isSet = true;
        this.energy = -1;
    }

    public float getxGravCent() {
        return xGravCent;
    }

    public void setxGravCent(float xGravCent) {
        this.xGravCent = xGravCent;
    }

    public float getyGravCent() {
        return yGravCent;
    }

    public void setyGravCent(float yGravCent) {
        this.yGravCent = yGravCent;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isSet() {
        if(isSet) {
            return true;
        }
        return false;
    }

    public void print() {
        System.out.println(xGravCent + " " + yGravCent);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public short getEnergyLevel() {
        return this.energy;
    }

    public void setEnergy(short e) {
        this.energy = e;
    }
}
