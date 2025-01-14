import javax.swing.*;
import java.awt.*;
// import java.awt.event.*;
// import javax.swing.event.*;

/**
 * A GUI for the environment, with runtime controls.
 * 
 * @author David J. Barnes and Michael Kölling
 * @version  2016.02.29
 */
public class EnvironmentView extends JFrame
{
    // The longest delay for the animation, in milliseconds.
    private static final int LONGEST_DELAY = 46; //2*23, speedslider takes 50% as default
    // Colors for the different cell states.
    private static final double WHITE_KEY_HEIGHT_TO_WIDTH = 3.562091503267974;
    private static final double BLACK_KEY_HEIGHT_TO_WIDTH = 3.783505154639175;
    private static final double BLACK_WHITE_WIDTH_RATIO   = 7.0/12;
    private final int NUM_WHITE_KEYS;

    private final int ORIGINAL_WHITE_HEIGHT;
    private TextArea chords;
    private GridView view;
    private final Environment env;
    private boolean running;
    private int delay;
    
    /**
     * Constructor for objects of class EnvironmentView
     * @param env
     */
    public EnvironmentView(Environment env, int rows, int cols)
    {
        super("Pianola aleatoria");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocation(20, 20);
        this.env = env;
        this.running = false;
        setDelay(50);
        setupControls();
        setupChords();
        NUM_WHITE_KEYS = 52; //tengo que hacer el calculo despues
        ORIGINAL_WHITE_HEIGHT = (int)(WHITE_KEY_HEIGHT_TO_WIDTH*cols/NUM_WHITE_KEYS);
        setupGrid(rows, cols);
        pack();
        setVisible(true);
        super.setBackground(Color.BLACK);
    }

    private void setupChords() {
        chords = new TextArea("Acorde _ \n 1. A \n 2. A \n 3. A", 3, 20, TextArea.SCROLLBARS_NONE);
        chords.setEditable(false);
        chords.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        getContentPane().add(chords, BorderLayout.NORTH);
    }

    private void updateChords(int numAcorde, String acorde1, String acorde2, String acorde3) {
        chords.setText("Acorde " + numAcorde + ":\n   1. " + acorde1 + "\n   2. " + acorde2 + "\n   3. " + acorde3);
    }

    /**
     * Setup a new environment of the given size.
     * @param rows The number of rows.
     * @param cols The number of cols;
     */
    private void setupGrid(int rows, int cols)
    {
        Container contents = getContentPane();
        view = new GridView(rows, cols);
        contents.add(view, BorderLayout.CENTER);
    }
    
    /**
     * Show the states of the cells.
     */
    public void showCells()
    {
        Cell[][] cells = env.getCells();
        if(!isVisible()) {
            setVisible(true);
        }
        
        view.preparePaint();
        
        for(int row = 0; row < cells.length; row++) {
            int numCols = cells[row].length;
            for(int col = 0; col < numCols; col++) {
                Color color = cells[row][col].getColor();
                view.drawMark(col, row, color);
            }
        }
        

        // El teclado
        for (int i=0; i<NUM_WHITE_KEYS; i++) {
            int c = (i/7)*12;
            switch (i%7) {
                case 0: c += 0; break;
                case 1: c += 2; break;
                case 2: c += 3; break;
                case 3: c += 5; break;
                case 4: c += 7; break;
                case 5: c += 8; break;
                case 6: c += 10; break;
            }
            view.drawWhiteKey(i, 0, cells[0][c].getVolume()>0);
        }

        for (int col = 0; col < cells[0].length; col++) {
            if (col % 12 == 1 || col % 12 == 4 || col % 12 == 6 || col % 12 == 9 || col % 12 == 11){
                view.drawBlackKey(col, 0);  
            }
        }

        //los volumenes
        for (int col =0; col < cells[0].length; col++) {
            view.drawVolume(col, 0, Color.BLACK, 100);
            view.drawVolume(col, 0, cells[0][col].getColor(), cells[0][col].getVolume()*100/255);
        }

        view.repaint();
    }
    
    /**
     * Set up the animation controls.
     */
    private void setupControls()
    {
        // Continuous running.
        final JButton run = new JButton("Run");
        run.addActionListener(e -> {
            if(!running) {
                running = true;
                try {
                    new Runner().execute();
                }
                catch(Exception ex) {
                }
            }
        });
        
        // Single stepping.
        final JButton step = new JButton("Step");
        step.addActionListener(e -> {
            running = false;
            env.step();
            updateChords((int)(Math.random() * 100), "" +Math.random(), "" +Math.random(), "" +Math.random());
            showCells();
        });
        
        // Pause the animation.
        final JButton pause = new JButton("Pause");
        pause.addActionListener(e -> running = false);
        
        // Reset of the environment
        final JButton reset = new JButton("Reset");
        reset.addActionListener(e -> {
            running = false;
            env.reset();
            showCells();
        });
        

        Container contents = getContentPane();

        
        // A speed controller.
        final JSlider speedSlider = new JSlider(0, 100);
        speedSlider.addChangeListener(e -> {
            setDelay(speedSlider.getValue());
        });
        Container speedPanel = new JPanel();
        speedPanel.setLayout(new GridLayout(2, 1));
        speedPanel.add(new JLabel("Animation Speed", SwingConstants.CENTER));
        speedPanel.add(speedSlider);
        contents.add(speedPanel, BorderLayout.NORTH);
        
        // Place the button controls.
        JPanel controls = new JPanel();
        controls.add(run);
        controls.add(step);
        controls.add(pause);
        controls.add(reset);
        
        contents.add(controls, BorderLayout.SOUTH);
    }

    
    /**
     * Set the animation delay.
     * @param speedPercentage (100-speedPercentage) as a percentage of the LONGEST_DELAY.
     */
    private void setDelay(int speedPercentage)
    {
        delay = (int) ((100.0 - speedPercentage) * LONGEST_DELAY / 100);
    }
    
    /**
     * Provide stepping of the animation.
     */
    private class Runner extends SwingWorker<Boolean, Void>
    {
        @Override
        /**
         * Repeatedly single-step the environment as long
         * as the animation is running.
         */
        public Boolean doInBackground()
        {
            while(running) {
                env.step();
                showCells();
                try {
                    Thread.sleep(delay);
                }
                catch(InterruptedException e) {
                }
            }
            return true;
        }
    }


    /**
     * Provide a graphical view of a rectangular grid.
     */
    @SuppressWarnings("serial")
    private class GridView extends JPanel
    {
        private static final int GRID_VIEW_X_SCALING_FACTOR = 8;
        private static final int GRID_VIEW_Y_SCALING_FACTOR = 5;


        private final int gridWidth, gridHeight;
        private double xScale, yScale, whiteWidth, blackWidth, whiteHeight, blackHeight;
        private Dimension size;
        private Graphics g;
        private Image fieldImage;

        /**
         * Create a new GridView component.
         */
        public GridView(int height, int width)
        {
            gridHeight = height;
            gridWidth = width;
            size = new Dimension(0, 0);
        }

        /**
         * Tell the GUI manager how big we would like to be.
         */
        @Override
        public Dimension getPreferredSize()
        {
            return new Dimension(gridWidth * GRID_VIEW_X_SCALING_FACTOR,
                                 (int)((gridHeight+2*ORIGINAL_WHITE_HEIGHT) * GRID_VIEW_Y_SCALING_FACTOR));
        }

        /**
         * Prepare for a new round of painting. Since the component
         * may be resized, compute the scaling factor again.
         */
        public void preparePaint()
        {
            if(! size.equals(getSize())) {
                size = getSize();
                
                fieldImage = view.createImage(size.width, size.height);
                g = fieldImage.getGraphics();
                
                whiteWidth = (double)size.width/NUM_WHITE_KEYS;  
                blackWidth = whiteWidth*BLACK_WHITE_WIDTH_RATIO;     
                whiteHeight = whiteWidth*WHITE_KEY_HEIGHT_TO_WIDTH;
                blackHeight = blackWidth*BLACK_KEY_HEIGHT_TO_WIDTH;    
                // xScale = (double)(size.width-(whiteWidth-blackWidth)-blackWidth/2) / gridWidth;     
                xScale = blackWidth;     
                if(xScale < 1) {
                    xScale = GRID_VIEW_X_SCALING_FACTOR;
                    whiteWidth = GRID_VIEW_X_SCALING_FACTOR*gridWidth/NUM_WHITE_KEYS;
                    blackWidth = (GRID_VIEW_X_SCALING_FACTOR*gridWidth*137)/(235*NUM_WHITE_KEYS);
                }
                yScale = ((double)size.height - 2*whiteHeight)/ gridHeight;
                if(yScale < 1) {
                    yScale = GRID_VIEW_Y_SCALING_FACTOR;
                }
            }
            
        }
        
        /**
         * Paint on grid location on this field in a given color.
         */
        public void drawBlackKey(int x, int y)
        {
            int finalBlackWidth = (int)((x+1)*blackWidth) > (int)(x*blackWidth)+(int)blackWidth ? (int)blackWidth+1 : (int)blackWidth;
            g.setColor(Color.BLACK);
            g.fillRect((int)((x+0.5) * blackWidth), (int)whiteHeight, (int)finalBlackWidth, (int)blackHeight);
        }
        
        public void drawWhiteKey(int x, int y, boolean pressed)
        {
            int finalWhiteWidth = (int)((x+1)*whiteWidth) > (int)(x*whiteWidth)+(int)whiteWidth ? (int)whiteWidth : (int)whiteWidth-1;
            g.setColor(pressed ? new Color(0.5f, 0.5f, 0.5f): Color.WHITE);;
            g.fillRect((int)(x * whiteWidth)+1, (int)whiteHeight, finalWhiteWidth, (int)(whiteHeight));
        }
        /**
         * Paint on grid location on this field in a given color.
         */
        public void drawMark(int x, int y, Color color)
        {
            int finalSideWidth = (int)((x+1)*xScale+ blackWidth/2) > (int)(x*xScale+ blackWidth/2)+(int)xScale ? (int)xScale+1 : (int)xScale;
            int finalSideHeight = (int)((y+1)*yScale) > (int)(y*yScale)+(int)yScale+1 ? (int)yScale+2 : (int)yScale+1;
            g.setColor(color);
            g.fillRect((int)((x+0.5) * xScale), (int)(y * yScale+2*whiteHeight), (int)finalSideWidth, (int)finalSideHeight);
        }
        
        public void drawVolume(int x, int y, Color color, double heightPercentage) {
            int finalSideWidth = (int)((x+1)*xScale+ blackWidth/2) > (int)(x*xScale+ blackWidth/2)+(int)xScale ? (int)xScale+1 : (int)xScale;
            g.setColor(color);
            g.fillRect((int)((x+0.5)*xScale), (int)(y*yScale+whiteHeight*(1-heightPercentage/100)), finalSideWidth, (int)(heightPercentage*whiteHeight/100));
        }


        /**
         * The field view component needs to be redisplayed. Copy the
         * internal image to screen.
         */
        @Override
        public void paintComponent(Graphics g)
        {
            
            
            if(fieldImage != null) {
                Dimension currentSize = getSize();
                if(size.equals(currentSize)) {            
                    g.drawImage(fieldImage, 0, 0, null);
                }
                else {
                    // Rescale the previous image.
                    g.drawImage(fieldImage, 0, 0, currentSize.width, currentSize.height, null);
                }
            
            }
                
            
        }
    }
}