package app.controller;


import app.models.Cell;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

enum BoundaryCondition {
    PERIODIC,
    ABSORBING
}

enum Nucleation {
    HOMOGENEOUS,
    RANDOM,
    RADIUS,
    CLEAR
}

enum Neighbour {
    MOORE,
    NEUMANN,
    PENTAGONAL,
    HEXAGONAL,
    RADIUS,
    HEXAGONAL_LEFT,
    HEXAGONAL_RIGHT
}

public class GrainGrowth {
    private Integer gridWidth = 50;
    private Integer gridHeight = 50;
    private Cell gridSpace[][];

    private BoundaryCondition boundaryCondition = BoundaryCondition.PERIODIC;
    private Integer grainNumber = 5;
    private Integer grainNumberExtra = 0;
    private Nucleation nucleation = Nucleation.HOMOGENEOUS;
    private Integer nuclRadius = 1;
    private Neighbour neighbourhood = Neighbour.MOORE;
    private Integer neighRadius = 1;
    // Hashset for colors
    HashSet<Color> colorSet = new HashSet<>();
    // Is last iteration

    // Should simulation be aborted
    private boolean shutItDown = false;

    // For MC method
    private boolean mcOptimization = false;
    private short mcIterations = 0;
    private float mcKt = 0.1f;

    public void setMcOptimization(short iterations, float mcKt) {
        this.mcOptimization = true;
        this.mcIterations = iterations;
        this.mcKt = mcKt;
    }


    // ID For cell groups
    private Integer lastId;

    // Constructor
    public GrainGrowth(int width, int height, String bcond, int grain, int grainE, String nuc, int nucr, String neigh, int neighr) {
        this.gridWidth = width;
        this.gridHeight = height;
        this.grainNumber = grain;
        this.nuclRadius = nucr;
        this.neighRadius = neighr;
        this.lastId = 0;
        this.grainNumberExtra = grainE;

        switch(bcond) {
            case "Periodyczne":
                this.boundaryCondition = BoundaryCondition.PERIODIC;
                break;
            case "Absorbujące":
                this.boundaryCondition = BoundaryCondition.ABSORBING;
                break;
            default:
                this.boundaryCondition = BoundaryCondition.PERIODIC;
                break;
        }

        switch(nuc) {
            case "Jednorodne":
                this.nucleation = Nucleation.HOMOGENEOUS;
                break;
            case "Losowe":
                this.nucleation = Nucleation.RANDOM;
                break;
            case "Z promieniem":
                this.nucleation = Nucleation.RADIUS;
                break;
            case "Pusta":
                this.nucleation = Nucleation.CLEAR;
                break;
        }

        switch(neigh) {
            case "Moore":
                this.neighbourhood = Neighbour.MOORE;
                break;
            case "Von Neumann":
                this.neighbourhood = Neighbour.NEUMANN;
                break;
            case "Pentagonalne losowe":
                this.neighbourhood = Neighbour.PENTAGONAL;
                break;
            case "Heksagonalne losowe":
                this.neighbourhood = Neighbour.HEXAGONAL;
                break;
            case "Heksagonalne prawe":
                this.neighbourhood = Neighbour.HEXAGONAL_LEFT;
                break;
            case "Heksagonalne lewe":
                this.neighbourhood = Neighbour.HEXAGONAL_RIGHT;
                break;
            case "Z promieniem":
                this.neighbourhood = Neighbour.RADIUS;
                break;
        }

    }

    public GrainGrowth() {
        this.gridWidth = 50;
        this.gridHeight = 50;
        this.grainNumber = 6;
        this.nuclRadius = 1;
        this.neighRadius = 1;
        this.lastId = 0;
    }

    public void setupGrid() {
        Cell[][] gridClone = new Cell[this.gridHeight][this.gridWidth];

        // setup initial cell status id to 0 and white color
        for(int i = 0; i<this.gridHeight; i++) {
            for(int j=0; j<this.gridWidth; j++) {
                Double xrand = ThreadLocalRandom.current().nextDouble(-1.,1.);
                Double yrand = ThreadLocalRandom.current().nextDouble(-1., 1.);
                Cell helper = new Cell(Color.WHITE, (float) (xrand + i), (float) (yrand + j), 0);
                gridClone[i][j] = helper;
            }
        }


        switch (this.nucleation) {
            case HOMOGENEOUS:

                int widthOffset = (this.gridWidth)/(this.grainNumber)+1;
                int heightOffset = (this.gridHeight)/(this.grainNumberExtra)+1;

                for(int i = widthOffset/4; i<this.gridHeight; i= heightOffset+i) {
                    //System.out.println(heightOffset);
                    for(int j =heightOffset/4; j<this.gridWidth; j = widthOffset+j) {
                        Color col =  this.selectColor();
                        gridClone[i][j].setupCell(lastId+1, col);
                        this.lastId = lastId+1;
                    }
                }
                break;
            case RADIUS:

                List<Float> xList = new ArrayList<>();
                List<Float> yList = new ArrayList<>();
                int iterator = 0;
                // infinite loop prevention
                int failIterator = 0;

                while(iterator < this.grainNumber) {
                    boolean isGood = true;

                    Color col =  this.selectColor();
                    int a = ThreadLocalRandom.current().nextInt(0, this.gridHeight);
                    int b = ThreadLocalRandom.current().nextInt(0, this.gridWidth);

                    Cell helper = gridClone[a][b];


                    if(xList.size() > 0) {

                        for(int k=0; k<xList.size(); k++) {
                            double dif = Math.sqrt(Math.pow(xList.get(k)-helper.getxGravCent(),2) + Math.pow(yList.get(k)-helper.getyGravCent(),2));
                            if (dif < this.nuclRadius) {
                                //System.out.println(dif);
                                isGood = false;
                            }
                        }
                    }

                    if(isGood) {
                        xList.add(helper.getxGravCent());
                        yList.add(helper.getyGravCent());
                        gridClone[a][b].setupCell(lastId+1, col);
                        this.lastId = lastId+1;
                        iterator++;
                    }
                    else {
                        failIterator++;
                        if(failIterator==10000) iterator = this.grainNumber;
                    }
                }
                break;
            case RANDOM:
                for(int i = 0; i<this.grainNumber; i++) {
                    Color col =  this.selectColor();
                    int a = ThreadLocalRandom.current().nextInt(0, this.gridHeight);
                    int b = ThreadLocalRandom.current().nextInt(0, this.gridWidth);
                    gridClone[a][b].setupCell(lastId+1, col);
                    this.lastId = lastId+1;
                }
                break;
            case CLEAR:

                break;
        }
        this.gridSpace = gridClone;
    }

    // Returns neighbours
    private List<Cell> checkNeighbour(Cell[][] gridClone, int i, int j) {
        List<Cell> neighbours = new ArrayList<>();
        int xo = this.gridHeight;
        int yo = this.gridWidth;

        // Neighbourhood types
        switch (this.neighbourhood) {
            case MOORE:

                if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                    neighbours.add(gridClone[(i+xo-1)%xo][(j+yo)%yo]);
                    neighbours.add(gridClone[(i+xo-1)%xo][(j+yo+1)%yo]);
                    neighbours.add(gridClone[(i+xo)%xo][(j+yo+1)%yo]);
                    neighbours.add(gridClone[(i+xo+1)%xo][(j+yo+1)%yo]);
                    neighbours.add(gridClone[(i+xo+1)%xo][(j+yo)%yo]);
                    neighbours.add(gridClone[(i+xo+1)%xo][(j+yo-1)%yo]);
                    neighbours.add(gridClone[(i+xo)%xo][(j+yo-1)%yo]);
                    neighbours.add(gridClone[(i+xo-1)%xo][(j+yo-1)%yo]);
                }
                else {
                    for(int k=-1; k<2; k++) {
                        for(int l=-1; l<2; l++) {
                            try {
                                neighbours.add(gridClone[k+i][l+j]);
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                //neighbours.add(new Cell(Color.WHITE, 0,0 ,0));
                            }
                        }
                    }
                }

                break;
            case NEUMANN:
                if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                    neighbours.add(gridClone[(i+xo-1)%xo][(j+yo)%yo]);
                    neighbours.add(gridClone[(i+xo)%xo][(j+yo+1)%yo]);
                    neighbours.add(gridClone[(i+xo+1)%xo][(j+yo)%yo]);
                    neighbours.add(gridClone[(i+xo)%xo][(j+yo-1)%yo]);
                }
                else {
                    for(int k=-1; k<2; k++) {
                        for(int l=-1; l<2; l++) {
                            if((k==l && k==-1) || (k==1 && l==-1) || (k==l && k==1) || (k==-1 && l==1))
                                continue;
                            try {
                                neighbours.add(gridClone[k+i][l+j]);
                            }
                            catch (ArrayIndexOutOfBoundsException e) {
                                //neighbours.add(new Cell(Color.WHITE, 0,0 ,0));
                            }
                        }
                    }
                }
                break;

            case HEXAGONAL:
                // Wprowadzenie losowosci
                int hexDirection = ThreadLocalRandom.current().nextInt(0, 2);
                boolean hexCond = true;

                for(int k=-1; k<2; k++) {
                    for(int l=-1; l<2; l++) {
                        if(hexDirection==0) {
                            hexCond = ((k==1 && l==-1) || (k==-1 && l==1));
                        }
                        else {
                            hexCond = ((k==-1 && l==-1) || (k==1 && l==1));
                        }


                        if(hexCond)
                            continue;

                        if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                            neighbours.add(gridClone[(k+i+xo)%xo][(l+j+yo)%yo]);
                        }
                        else {
                            try {
                                neighbours.add(gridClone[k + i][l + j]);
                            } catch (ArrayIndexOutOfBoundsException er) {
                                //neighbours.add(new Cell(Color.WHITE, 0, 0, 0));
                            }
                        }
                    }
                }
                break;

            case HEXAGONAL_LEFT:

                for(int k=-1; k<2; k++) {
                    for(int l=-1; l<2; l++) {

                        if(((k==1 && l==-1) || (k==-1 && l==1)))
                            continue;

                        if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                            neighbours.add(gridClone[(k+i+xo)%xo][(l+j+yo)%yo]);
                        }
                        else {
                            try {
                                neighbours.add(gridClone[k + i][l + j]);
                            } catch (ArrayIndexOutOfBoundsException er) {
                                //neighbours.add(new Cell(Color.WHITE, 0, 0, 0));
                            }
                        }
                    }
                }
                break;


            case HEXAGONAL_RIGHT:
                for(int k=-1; k<2; k++) {
                    for(int l=-1; l<2; l++) {

                        if((k==-1 && l==-1) || (k==1 && l==1))
                            continue;

                        if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                            neighbours.add(gridClone[(k+i+xo)%xo][(l+j+yo)%yo]);
                        }
                        else {
                            try {
                                neighbours.add(gridClone[k + i][l + j]);
                            } catch (ArrayIndexOutOfBoundsException er) {
                                //neighbours.add(new Cell(Color.WHITE, 0, 0, 0));
                            }
                        }
                    }
                }
                break;


            case PENTAGONAL:
                // Wprowadzenie losowosci
                int direction = ThreadLocalRandom.current().nextInt(0, 4);
                boolean cond = true;

                for(int k=-1; k<2; k++) {
                    for(int l=-1; l<2; l++) {
                        if(direction==0) {
                            cond = ((k==l && k==1) || (k==0 && l==1) || (k==-1 && l==1));
                        }
                        else if(direction==1){
                            cond = ((k==l && k==-1) || (k==0 && l==-1) || (k==1 && l==-1));
                        }
                        else if(direction==2){
                            cond = ((k==l && k==1) || (k==1 && l==0) || (k==1 && l==-1));
                        }
                        else {
                            cond = ((k==l && k==-1) || (k==-1 && l==0) || (k==-1 && l==1));
                        }

                        if(cond)
                            continue;

                        if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                            neighbours.add(gridClone[(k+i+xo)%xo][(l+j+yo)%yo]);
                        }
                        else {
                            try {
                                neighbours.add(gridClone[k + i][l + j]);
                            } catch (ArrayIndexOutOfBoundsException er) {
                                //neighbours.add(new Cell(Color.WHITE, 0, 0, 0));
                            }
                        }
                    }
                }

                break;

            case RADIUS:
                for(int k=-1*this.neighRadius; k<this.neighRadius+1; k++) {
                    for(int l=-1*this.neighRadius; l<this.neighRadius+1; l++) {
                        if(this.boundaryCondition == BoundaryCondition.PERIODIC) {
                            neighbours.add(gridClone[(k+i+xo)%xo][(l+j+yo)%yo]);
                        }
                        else {
                            try {
                                neighbours.add(gridClone[k + i][l + j]);
                            } catch (ArrayIndexOutOfBoundsException er) {
                                //neighbours.add(new Cell(Color.WHITE, 0, 0, 0));
                            }
                        }
                    }
                }
                break;
        }


        return neighbours;
    }

    // Determines cell state
    private Cell determineCellState(Cell cell, List<Cell> neighbours) {

        if(cell.getId() != 0) {
            return cell;
        }

        Cell newCell = cell;
        List<Integer> id = new ArrayList<>();


        for(int i =0; i<neighbours.size(); i++) {
            if(neighbours.get(i).getId() != 0) {
                id.add(neighbours.get(i).getId());
            }
        }

        // filters out neighbors not in radial
        if(this.neighbourhood == Neighbour.RADIUS) {
            neighbours = filterOutRadius(neighbours, cell);
        }

        if(id.size() > 0) {
            int popularElement = getPopularElement(id);
            Color col = null;
            for(int i =0; i<neighbours.size(); i++) {
                if(neighbours.get(i).getId() == popularElement) {
                    col = neighbours.get(i).getColor();
                }
            }
            newCell = new Cell(col, cell.getxGravCent(), cell.getyGravCent(), popularElement);
        }

        return newCell;
    }

    public List<Cell> filterOutRadius(List<Cell> neighbours, Cell cell) {

        List<Cell> newNeighbours = new ArrayList<>();

        for(int i =0; i<neighbours.size(); i++) {
            double dif = Math.sqrt(Math.pow(cell.getxGravCent()-neighbours.get(i).getxGravCent(),2) + Math.pow(cell.getyGravCent()-neighbours.get(i).getyGravCent(),2));
            if(dif <= this.neighRadius) {
                //System.out.println(neighbours.get(i).getColor());
                newNeighbours.add(neighbours.get(i));
            }
        }

        return newNeighbours;
    }

    public int getPopularElement(List<Integer> a)
    {
        int count = 1, tempCount;
        int popular = a.get(0);
        int temp = 0;
        for (int i = 0; i < (a.size() - 1); i++)
        {
            temp = a.get(i);
            tempCount = 0;
            for (int j = 1; j < a.size(); j++)
            {
                if (temp == a.get(j))
                    tempCount++;
            }
            if (tempCount > count)
            {
                popular = temp;
                count = tempCount;
            }
        }
        return popular;
    }

    private short determineEnergy(Cell cell, List<Cell> neighbours) {

        short energyLevel = 0;
        if(cell.getId() == 0) {
            return 0;
        }

        int cellId = cell.getId();
        for(int i=0;i<neighbours.size(); i++) {
            if(neighbours.get(i).getId() != cellId) energyLevel++;
        }

        return energyLevel;
    }


    private void setEnergy() {
        for(int i = 0; i<this.gridHeight; i++) {
            for(int j=0; j<this.gridWidth; j++) {
                this.gridSpace[i][j].setEnergy(determineEnergy(this.gridSpace[i][j], checkNeighbour(this.gridSpace, i,j)));
            }
        }
    }

    private void monteCarloOptimize(float kt) {
        Cell[][] next = new Cell[this.gridHeight][this.gridWidth];
        for(int i=0; i<gridHeight; i++) {
            for(int j=0; j<gridWidth; j++) {
                next[i][j] = this.gridSpace[i][j];
            }
        }

        // Step 2
        // Create list of cells id and shuffle it
        List<Integer[]> cells = new ArrayList<>();

        for(int i = 0; i<this.gridHeight; i++) {
            for(int j=0; j<this.gridWidth; j++) {
                Integer[] temp = {i,j};
                cells.add(temp);
            }
        }
        Collections.shuffle(cells);

        cells.forEach((element)-> {
            int k = element[0];
            int l = element[1];
            // Step 3
            List<Cell> neighbours = checkNeighbour(gridSpace, k, l);
            short energyInitial = determineEnergy(gridSpace[k][l], neighbours);

            // Step 4
            Cell cellCopy = gridSpace[k][l];
            // Randomly replace original id with one of it's neighbours id
            Cell randomCell =  randomlySwapId(k, l, neighbours);

            // Step 5
            short energyAfterSwap = randomCell.getEnergyLevel();
            //System.out.println(energyAfterSwap-energyInitial + "i: " + k + " j: " + l);

            // Step 6
            if(energyAfterSwap-energyInitial>0) {
                float probability = (float) Math.exp(-(energyAfterSwap - energyInitial) / kt);
                // losowanie
                Double selector = ThreadLocalRandom.current().nextDouble(0, 1.0);
                if(selector > probability) {
                    gridSpace[k][l].setEnergy(cellCopy.getEnergyLevel());
                    gridSpace[k][l].setColor(cellCopy.getColor());
                    gridSpace[k][l].setId(cellCopy.getId());
                }
            }
            else {
                gridSpace[k][l].setEnergy(randomCell.getEnergyLevel());
                gridSpace[k][l].setColor(randomCell.getColor());
                gridSpace[k][l].setId(randomCell.getId());
            }
            this.setEnergy();
        });

        //this.gridSpace = next;
    }

    private Cell randomlySwapId(int i, int j, List<Cell> neighbours) {
        float originalX = this.gridSpace[i][j].getxGravCent();
        float originalY = this.gridSpace[i][j].getyGravCent();
        Cell cell = null;

        int rand = ThreadLocalRandom.current().nextInt(0, neighbours.size()-1);
        cell = neighbours.get(rand);
        cell.setxGravCent(originalX);
        cell.setyGravCent(originalY);
        cell.setEnergy(determineEnergy(cell, neighbours));

        //this.gridSpace[i][j].setColor(cell.getColor());
        //this.gridSpace[i][j].setEnergy(determineEnergy(cell, neighbours));
        //this.gridSpace[i][j].setId(cell.getId());

        return cell;
    }

    // Single iteration
    public void iterate() {
        int zeroTracker = 0;

        Cell[][] next = new Cell[this.gridHeight][this.gridWidth];

        for(int i=0; i<gridHeight; i++) {
            for(int j=0; j<gridWidth; j++) {
                next[i][j] = this.gridSpace[i][j];
            }
        }

        for(int i = 0; i<this.gridHeight; i++) {
            for(int j=0; j<this.gridWidth; j++) {
                if(!shouldFinish()) {
                    next[i][j] = determineCellState(this.gridSpace[i][j], checkNeighbour(this.gridSpace, i, j));
                }
                if(next[i][j].getId() == 0) {
                    zeroTracker++;
                }
            }
        }

        this.gridSpace = next;

        if(zeroTracker==0) {
            this.setEnergy();
            System.out.println("Energy before MC: " + calculateTotalEnergy());;

            if(this.mcOptimization) {
                this.monteCarloOptimize(this.mcKt);
                this.mcIterations--;
                System.out.println(mcIterations);
                System.out.println("Energy after MC: " + calculateTotalEnergy());;
            }
            else {
                this.mcIterations = 0;
            }
            this.shutItDown = true;
        }
    }

    private int calculateTotalEnergy() {
        int energy = 0;

        for(int i = 0; i<this.gridHeight; i++) {
            for(int j=0; j<this.gridWidth; j++) {
                energy += determineEnergy(this.gridSpace[i][j], checkNeighbour(this.gridSpace, i,j));
            }
        }
        return energy;
    }

    private Color selectColor() {
        boolean isPresent = true;
        Color col = Color.WHITE;

        while(isPresent) {
            int R = ThreadLocalRandom.current().nextInt(50, 230);
            int G = ThreadLocalRandom.current().nextInt(50, 230);
            int B = ThreadLocalRandom.current().nextInt(50, 230);
            col = Color.rgb(R, G, B);
            if(!colorSet.contains(col)) {
                isPresent = false;
            }
        }
        colorSet.add(col);

        return col;
    }

    public Cell[][] getGridSpace() {
        return gridSpace;
    }

    public Color getColor(int i, int j) { return this.gridSpace[i][j].getColor();}

    public void setGridSpace(int i, int j, Color col) {
        Double xrand = ThreadLocalRandom.current().nextDouble(-1.,1.);
        Double yrand = ThreadLocalRandom.current().nextDouble(-1.,1.);
        Cell helper = new Cell(selectColor(), (float) (xrand + i), (float) (yrand + j), lastId+1);
        lastId++;
        this.gridSpace[i][j] = helper;
    }

    public boolean shouldFinish() {
        return this.shutItDown;
    }

    public int getId(int i, int j) {
        return this.gridSpace[i][j].getId();
    }

    public boolean getShutDown() {
        return this.shutItDown;
    }

    public int getMcIterations() {
        return this.mcIterations;
    }

    public short getEnergy(int i, int j) {
        return gridSpace[i][j].getEnergyLevel();
    }
}