package app.controller;

import app.models.Cell;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;

public class EnergyWindowController implements Initializable {

    @FXML
    private Canvas canvas;

    private int canvasHeight;
    private int canvasWidth;
    private GrainGrowth grainGrowth;
    private float maxEnergyLevel;

    static int recWidth = 1;

    // Updates grid
    public void updateGrid(short dimY, short dimX, Cell[][] spaceGrid) {
        if(dimX > dimY) {
            recWidth = 500 / dimX;
        }
        else {
            recWidth = 500 / dimY;
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.getCanvas().setHeight(dimY*recWidth);
        gc.getCanvas().setWidth(dimX*recWidth);

        for(int i = 0; i <dimX; i++) {
            for(int j=0; j<dimY; j++){
                if (spaceGrid != null && spaceGrid[j][i].isSet()) {
                    gc.fillRect(i*recWidth,j*recWidth,i*recWidth-recWidth,j*recWidth-recWidth);
                    gc.setFill(getColor((short)spaceGrid[j][i].getEnergyLevel()));
                }
            }

        }

        gc = null;
        System.gc();

    }

    public void setHighestEnergy(short dimY, short dimX, Cell[][] spaceGrid) {
        float maxEnergy = 0;

        for(int i = 0; i <dimX; i++) {
            for(int j=0; j<dimY; j++){
                if (spaceGrid != null && spaceGrid[j][i].isSet()) {
                    if(spaceGrid[j][i].getEnergyLevel() > maxEnergy) maxEnergy=spaceGrid[j][i].getEnergyLevel();
                }
            }
        }
        this.maxEnergyLevel = maxEnergy;
    }

    private Color getColor(short energyLevel) {
        Color color = Color.rgb(0,0,0);

        if(energyLevel==0) {
            color = Color.rgb(0, 0, 255);
        }
        else if(energyLevel==1){
            color = Color.rgb(0, 150, 0);
        }
        else if(energyLevel==2){
            color = Color.rgb(100, 150, 0);
        }
        else if(energyLevel==3){
            color = Color.rgb(150, 150, 0);
        }
        else if(energyLevel==4){
            color = Color.rgb(250, 250, 0);
        }
        else if(energyLevel==5){
            color = Color.rgb(250, 200, 0);
        }
        else if(energyLevel==6){
            color = Color.rgb(250, 150, 0);
        }
        else if(energyLevel==7){
            color = Color.rgb(250, 100, 0);
        }
        else if(energyLevel==8){
            color = Color.rgb(255, 50, 0);
        }
        else {
            color = Color.rgb(255, 0, 0).interpolate(Color.rgb(255, 255, 255), energyLevel / this.maxEnergyLevel);
        }

        return color;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.rgb(255,255,255));
        gc.fillRect(gc.getCanvas().getLayoutX(),
                gc.getCanvas().getLayoutY(),
                gc.getCanvas().getWidth(),
                gc.getCanvas().getHeight());
    }


    public void setCanvasHeight(short canvasHeight) {
        System.out.println(canvasHeight);
        this.canvasHeight = canvasHeight;
    }

    public void setCanvasWidth(short canvasWidth) {
        this.canvasWidth = canvasWidth;
    }

    public void setGrainGrowth(GrainGrowth grainGrowth) {
        this.grainGrowth = grainGrowth;
    }
}
