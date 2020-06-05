package app.controller;

import app.models.Cell;
import com.sun.prism.Graphics;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

public class AppController implements Initializable {

    // JavaFX variables
    @FXML
    private AnchorPane gridPane;
    @FXML
    private Button startButton;
    @FXML
    private TextField gridField;
    @FXML
    private TextField iterationField;
    @FXML
    private ChoiceBox<String> boundaryConditionType;
    @FXML
    private TextField numberOfGrains;
    @FXML
    private TextField numberOfGrainsExtra;
    @FXML
    private ChoiceBox<String> nucleationType;
    @FXML
    private TextField nucleationRadius;
    @FXML
    private ChoiceBox<String> neighbourhoodType;
    @FXML
    private TextField neighbourhoodRadius;
    @FXML
    private TextArea msgBox;
    @FXML
    private Canvas canvas;
    @FXML
    private CheckBox showEnergyBox;
    @FXML
    private CheckBox mcOptimizationCheckbox;
    @FXML
    TextField mcIterationsField;
    @FXML
    TextField mcKtField;

    // Rectangles represent single cells
    // Set up with max dimensions (95x95) in initialize()
    Rectangle[][] rec;
    // Rectangle width (and height)
    static int recWidth = 1;

    // Default values (used for generating initial empty grid and checking some conditions etc.)
    static final int defaultWidth = 500;
    static final int defaultLength = 500;
    // Variables for storing data fetched from gui
    private short gridWidth;
    private short gridLength;
    private String boundaryCondition;
    private short grainNumber;
    private short grainNumberExtra;
    private String nucleation;
    private short nuclRadius;
    private String neighbourhood;
    private short neighRadius;

    private boolean monteCarloMethod = false;
    private boolean showEnergy = false;
    private short mcIterations = 0;
    private float mckT = 0.0f;

    // Main object
    private GrainGrowth grainGrowth = new GrainGrowth();

    // Animation variables
    private boolean didUserUpdate = false;
    private boolean isStarted = false;

    // Shows step by step iterations
    Timeline timeline = new Timeline(new KeyFrame(Duration.millis(35), new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent event) {
            isStarted = true;
            if(grainGrowth.shouldFinish() == true && grainGrowth.getMcIterations() == 0) timeline.stop();

            grainGrowth.iterate();
            updateGrid(gridLength,gridWidth, grainGrowth.getGridSpace());

            if(grainGrowth.getShutDown() &&  grainGrowth.getMcIterations() == 0) {
                if(showEnergyBox.isSelected()) {
                    openEnergyWindow(grainGrowth);
                }
                timeline.stop();
            }
        }

    }));

    // Start button handler
    public void handleStart() {
        msgBox.setText("");

        // Get and validate params from GUI
        String gridString = gridField.getCharacters().toString();
        String iterationString = iterationField.getCharacters().toString();
        String grainNumberString = numberOfGrains.getCharacters().toString();
        String grainNumberStringExtra = numberOfGrainsExtra.getCharacters().toString();
        String nuclRadiusString = nucleationRadius.getCharacters().toString();
        String neighRadiusString = neighbourhoodRadius.getCharacters().toString();
        String mcIterationsString = mcIterationsField.getCharacters().toString();
        String mcKtString = mcKtField.getCharacters().toString();

        boundaryCondition = boundaryConditionType.getValue();
        nucleation = nucleationType.getValue();
        neighbourhood = neighbourhoodType.getValue();

        if(nuclRadiusString.isEmpty()) nuclRadiusString = "1";
        if(neighRadiusString.isEmpty()) neighRadiusString = "1";
        if(mcKtString.isEmpty()) mcKtString = "1";
        if(mcIterationsString.isEmpty()) mcIterationsString = "1";

        // Check for parse error
        try {
            gridWidth = (short) Integer.parseInt(gridString);
            gridLength = (short) Integer.parseInt(iterationString);
            grainNumber = (short) Integer.parseInt(grainNumberString);
            grainNumberExtra = (short) Integer.parseInt(grainNumberStringExtra);
            nuclRadius = (short) Integer.parseInt(nuclRadiusString);
            neighRadius = (short) Integer.parseInt(neighRadiusString);
            mckT = (float) Float.parseFloat(mcKtString);
            mcIterations = (short) Integer.parseInt(mcIterationsString);
        } catch (Exception e) {
            msgBox.setText("Parse error");
            System.err.println("Parse error");
            return;
        }

        // Check for empty params
        if(gridString.isEmpty() || iterationString.isEmpty()) {
            msgBox.setText("Nie podano wszystkich\nparametrów");
            System.err.println("Nie podano wszystkich parametrów");
            return;
        }

        // Check whether passed number of iterations or number of cells is valid
        if(gridWidth > defaultWidth || gridWidth < 2 || gridLength > defaultLength || gridLength < 2) {
            gridWidth = defaultWidth;
            gridLength = defaultLength;
            msgBox.setText("Przekroczono maks.\nwielkość siatki\n");
            System.err.println("Przekroczono maksymalna wielkosc siatki");
            return;
        }

        // Check whether mc params are viable
        if((mcIterations > 1000) || (mckT<0.1f) || (mckT>6.f)) {
            gridWidth = defaultWidth;
            gridLength = defaultLength;
            msgBox.setText("Błędne dane MC");
            System.err.println("Błędne dane MC");
            return;
        }

        /* logic here*/
        if(this.isStarted == false) {

            if(!didUserUpdate) {
                grainGrowth = new GrainGrowth(gridWidth, gridLength, boundaryCondition, grainNumber, grainNumberExtra, nucleation, nuclRadius, neighbourhood, neighRadius);
                grainGrowth.setupGrid();
            }
            if(mcOptimizationCheckbox.isSelected()) {
                grainGrowth.setMcOptimization(this.mcIterations, this.mckT);
            }
            updateGrid(this.gridLength,this.gridWidth, grainGrowth.getGridSpace());
            timeline.setCycleCount(Animation.INDEFINITE);
            startButton.setText("Zatrzymaj");
            timeline.play();
        }
        else {
            timeline.stop();
            startButton.setText("Wykonaj");
            this.isStarted = false;
            this.didUserUpdate = false;
        }


    }


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
                    gc.setFill(spaceGrid[j][i].getColor());
                }
            }

        }

        gc = null;
        System.gc();

    }

    // Handle click on anchor pane (drawing pane)
    // TODO if is started => return
    public void handleClick(MouseEvent mouseEvent) {
        double xPos = mouseEvent.getX();
        double yPos = mouseEvent.getY();

        List<Node> children = gridPane.getChildren();
        WeakReference<Node> rec = null;

        for (var child : children) {
            if(child.contains(xPos, yPos)) {
                rec = new WeakReference<>(child);
                break;
            }
        }

        int i = (int) yPos/recWidth;
        int j = (int) xPos/recWidth;

        try {
            if (i > this.gridLength - 1 || j > this.gridWidth - 1) {
                msgBox.setText("Zaznaczono obiekt poza\ngranicą przestrzeni");
                return;
            }
        } catch (Exception e) {
            return;
        }

        try {
            msgBox.setText("i: " + i + "\nj:" + j + "\nid: " + grainGrowth.getId(i, j) + "\nEnergy: " + grainGrowth.getEnergy(i,j));
            switchCell(i, j, rec);
            this.didUserUpdate = true;
        } catch (NullPointerException e) {
            System.out.println("Grid is not initialized");
        }

    }

    // updates cell with opposite to it's current value
    public void switchCell(int i, int j, WeakReference<Node> rec) {
        Color val = Color.RED;
        grainGrowth.setGridSpace(i, j, val);
        updateGrid(gridLength,gridWidth, grainGrowth.getGridSpace());
        System.gc();
    }

    @java.lang.Override
    public void initialize(java.net.URL url, java.util.ResourceBundle resourceBundle) {
        mcIterationsField.setDisable(true);
        mcKtField.setDisable(true);

        // Populates choiceboxes
        boundaryConditionType.getItems().addAll(
                "Periodyczne",
                "Absorbujące"
        );
        boundaryConditionType.setValue("Periodyczne");

        nucleationType.getItems().addAll(
            "Jednorodne",
                "Losowe",
                "Z promieniem",
                "Pusta"
        );
        nucleationType.setValue("Jednorodne");

        neighbourhoodType.getItems().addAll(
          "Moore",
                "Von Neumann",
                "Pentagonalne losowe",
                "Heksagonalne losowe",
                "Heksagonalne prawe",
                "Heksagonalne lewe",
                "Z promieniem"
        );
        neighbourhoodType.setValue("Moore");

        // Choicebox event listeners
        nucleationType.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if((Integer) t1 == 2) {
                    nucleationRadius.setDisable(false);
                }
                else {
                    nucleationRadius.setDisable(true);
                }
            }
        });

        neighbourhoodType.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if((Integer) t1 == 6) {
                    neighbourhoodRadius.setDisable(false);
                }
                else {
                    neighbourhoodRadius.setDisable(true);
                }
            }
        });


        // MC checkbox event listener
        mcOptimizationCheckbox.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if(mcOptimizationCheckbox.isSelected()) {
                    mcIterationsField.setDisable(false);
                    mcKtField.setDisable(false);
                }
                else {
                    mcIterationsField.setDisable(true);
                    mcKtField.setDisable(true);
                }
            }
        });

//Sets canvas background color
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.rgb(255,255,255));
        gc.fillRect(gc.getCanvas().getLayoutX(),
                gc.getCanvas().getLayoutY(),
                gc.getCanvas().getWidth(),
                gc.getCanvas().getHeight());

        nucleationRadius.setDisable(true);
        neighbourhoodRadius.setDisable(true);

        this.gridWidth = defaultWidth;
        this.gridLength = defaultLength;
    }


    public void openEnergyWindow(GrainGrowth grainGrowth) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("../views/energy.fxml"));
            loader.load();

            EnergyWindowController controller = loader.getController();
            controller.setHighestEnergy(this.gridLength, this.gridWidth, grainGrowth.getGridSpace());
            controller.updateGrid(this.gridLength, this.gridWidth, grainGrowth.getGridSpace());

            Parent parent = loader.getRoot();
            //Parent root = FXMLLoader.load(getClass().getResource("../views/energy.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Energy visualization");
            stage.setScene(new Scene(parent, 550, 550));
            stage.setResizable(false);
            stage.show();


        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

