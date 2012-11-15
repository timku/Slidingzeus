package slidingzeus;// ZeusPanel.java
/* The game's drawing surface. It shows:
     - the moving zeus
     - the obstacles (blue boxes)
     - the current average FPS and UPS

   The System timer is used to drive the animation loop.*/
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.imageio.ImageIO;

// import com.sun.j3d.utils.timer.J3DTimer;
public class ZeusPanel extends JPanel implements Runnable{
  private static final int PWIDTH = 800;   // size of panel
  private static final int PHEIGHT = 700;
  
  // private static long MAX_STATS_INTERVAL = 1000000000L;
  private static long MAX_STATS_INTERVAL = 1000L;
    // record stats every 1 second (roughly)

  private static final int NO_DELAYS_PER_YIELD = 16;
  /* Number of frames with a delay of 0 ms before the animation thread yields
     to other running threads. */

  private static int MAX_FRAME_SKIPS = 5;   // was 2;
    // no. of frames that can be skipped in any one animation loop
    // i.e the games state is updated but not rendered

  private static int NUM_FPS = 10;
     // number of FPS values stored to get an average

  public static boolean debugmode = false;
    
  // used for gathering statistics
  private long statsInterval = 0L;    // in ms
  private long prevStatsTime;
  private long totalElapsedTime = 0L;
  private long gameStartTime;
  private int timeSpentInGame = 0;    // in seconds

  private long frameCount = 0;
  private double fpsStore[];
  private long statsCount = 0;
  private double averageFPS = 0.0;

  private long framesSkipped = 0L;
  private long totalFramesSkipped = 0L;
  private double upsStore[];
  private double averageUPS = 0.0;


  private DecimalFormat df = new DecimalFormat("0.##");  // 2 dp
  private DecimalFormat timedf = new DecimalFormat("0.####");  // 4 dp


  private Thread animator;           // the thread that performs the animation
  private volatile boolean running = false;   // used to stop the animation thread
  private volatile boolean isPaused = false;

  private int period;                // period between drawing in _ms_


  private ZeusChase wcTop;
  //private Slidingzeus fred;// the worm, he died in a car accident.
  private ArrayList<Slidingzeus> mrZeus;

  private Image img_help;
  private boolean toggle_help=true;
  // used at game termination
  private volatile boolean gameOver = false;

  private String Winner="";
  private String highScore="";
  private int Winners=0;
  //private int score = 0;
  private Font font;
  private FontMetrics metrics;

  
  // off screen rendering
  private Graphics dbg;
  private Image dbImage = null;

  private Color Player_Color[] ={Color.BLUE,Color.RED,Color.GREEN,Color.CYAN,Color.ORANGE,Color.YELLOW};
  private String Players_Name[]={     "BLUE",    "RED",    "GREEN",    "CYAN",    "ORANGE",    "YELLOW"};
  private int Players = 1;
  
  private long timeNow;
  
  public ZeusPanel(ZeusChase wc, int period){
    wcTop = wc;
    this.period = period;
    this.mrZeus = new ArrayList<Slidingzeus>();
        try {
            img_help = ImageIO.read(getClass().getResource("keyboard.png"));
        } catch (IOException ex) {
            Logger.getLogger(ZeusPanel.class.getName()).log(Level.SEVERE, null, ex);
        }

    setBackground(Color.white);
    setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

    setFocusable(true);
    requestFocus();    // the JPanel now has focus, so receives key events
    readyForTermination();

    font = new Font("System", Font.BOLD, 14);
    metrics = this.getFontMetrics(font);

    // initialise timing elements
    fpsStore = new double[NUM_FPS];
    upsStore = new double[NUM_FPS];
    for(int i=0; i < NUM_FPS; i++){
      fpsStore[i] = 0.0;
      upsStore[i] = 0.0;
    }
  }  // end of ZeusPanel()



  private void readyForTermination(){
	addKeyListener( new KeyAdapter() {
	// listen for esc, q, end, ctrl-c on the canvas to
	// allow a convenient exit from the full screen configuration
       public void keyPressed(KeyEvent e)
       { int keyCode = e.getKeyCode();
         if((keyCode == KeyEvent.VK_ESCAPE)||((keyCode == KeyEvent.VK_C) && e.isControlDown()) ) {
           running = false;
           System.exit(0);
         }else if (keyCode == KeyEvent.VK_P) {
            if(isPaused==false){isPaused=true;}else{isPaused=false;};
         }
         if(gameOver){
             if(keyCode == KeyEvent.VK_F1&&gameOver){
                Reset();
             }else if(keyCode == KeyEvent.VK_1){//
               ChangePlayerAmount(1);
             }else if(keyCode == KeyEvent.VK_2){
               ChangePlayerAmount(2);
             }else if(keyCode == KeyEvent.VK_3){
               ChangePlayerAmount(3);
             }else if(keyCode == KeyEvent.VK_H){
               toggle_help = !toggle_help;
             }
         }else{
             if(keyCode == KeyEvent.VK_UP){// PLAYER 1
               ChangeDir(0,0);
             }else if(keyCode == KeyEvent.VK_DOWN){
               ChangeDir(2,0);// backwards
             }else if(keyCode == KeyEvent.VK_LEFT){
               ChangeDir(3,0);
             }else if(keyCode == KeyEvent.VK_RIGHT){
               ChangeDir(1,0);
             }else if(keyCode == KeyEvent.VK_W){// PLAYER 2
               ChangeDir(0,1);
             }else if(keyCode == KeyEvent.VK_S){
               ChangeDir(2,1);
             }else if(keyCode == KeyEvent.VK_A){
               ChangeDir(3,1);
             }else if(keyCode == KeyEvent.VK_D){
               ChangeDir(1,1);
             }else if(keyCode == KeyEvent.VK_I){// PLAYER 3
               ChangeDir(0,2);
             }else if(keyCode == KeyEvent.VK_K){
               ChangeDir(2,2);
             }else if(keyCode == KeyEvent.VK_J){
               ChangeDir(3,2);
             }else if(keyCode == KeyEvent.VK_L){
               ChangeDir(1,2);
             }
       }
     }
    });
  }  // end of readyForTermination()
    private void ChangePlayerAmount(int Players){
        if(this.Players != Players){
            this.Players = Players;
            HardReset();
        }
    }
  /*private void ChangeDir(int dir){
    ChangeDir(dir,-1);
  }//*/
  private void ChangeDir(int dir,int user){
    if(Players<=user)
        return;
    if(user == -1){
      for(Slidingzeus oneZeus : this.mrZeus){
        oneZeus.key_bearing = dir;
      }
    }else{
        int bearing = mrZeus.get(user).key_bearing;
          if(dir == 0&&bearing == 2){//TODO: fix this in class slidingzeus
              dir = 2;
          }else if(dir == 3&&bearing == 1){
              dir = 1;
          }else if(dir == 2&&bearing == 0){
              dir = 0;
          }else if(dir == 1&&bearing == 3){
              dir = 3;
          }
        mrZeus.get(user).key_bearing = dir;
    }
  }

  public void addNotify()
  // wait for the JPanel to be added to the JFrame before starting
  { super.addNotify();   // creates the peer
    startGame();    // start the thread
  }
  
  private void startGame(){
  // initialise and start the thread
    if(animator == null || !running) {
       animator = new Thread(this);
       animator.start();
    }
  } // end of startGame()


  // ------------- game life cycle methods ------------
  // called by the JFrame's window listener methods
  public void resumeGame(){
    // called when the JFrame is activated / deiconified
    isPaused = false;
  }
  public void pauseGame(){
    // called when the JFrame is deactivated / iconified
    isPaused = true;
  }
  public void stopGame(){
    // called when the JFrame is closing
    running = false;
  }
  // ----------------------------------------------

 public void Reset(){
    
    toggle_help=true;
    for(Slidingzeus oneZeus : this.mrZeus){
      oneZeus.Reset();
    }
    gameStartTime = System.currentTimeMillis();
    prevStatsTime = gameStartTime;
    gameOver = false;
 }
 public void HardReset(){
    mrZeus.clear();//Remove all zeus from screen.
    for(int i=1; i <= Players; i++){
      CreateZeus((PWIDTH/4)*i,PHEIGHT/2);//Left
    }
    Reset();
 }
 public void CreateZeus(int spawnX,int spawnY){
     if(mrZeus.size() < (Player_Color.length)){//do we have enough colors?
       this.mrZeus.add(new Slidingzeus(PWIDTH,PHEIGHT,spawnX,spawnY,Player_Color[mrZeus.size()]));
     }else{
       System.out.println("Error: too many Zeus spawning, need more colors!");
     }
 }

  public void run(){
  /* The frames of the animation are drawn inside the while loop. */
    long beforeTime, afterTime, timeDiff, sleepTime;
    int overSleepTime = 0;
    int noDelays = 0;
    int excess = 0;
    Graphics g;
    HardReset();
    beforeTime = gameStartTime;

    running = true;
    while(running){
        gameUpdate();
        gameRender();   // render the game to a buffer
        paintScreen();  // draw the buffer on-screen
        afterTime = System.currentTimeMillis();
        timeDiff = afterTime - beforeTime;
        sleepTime = (period - timeDiff) - overSleepTime;
        
    if(sleepTime > 0){   // some time left in this cycle
        try{
            Thread.sleep(sleepTime);  // already in ms
        }catch(InterruptedException ex){}
        overSleepTime = (int)((System.currentTimeMillis() - afterTime) - sleepTime);
    }else{// sleepTime <= 0; the frame took longer than the period
        excess -= sleepTime;// store excess time value
        overSleepTime = 0;

        if(++noDelays >= NO_DELAYS_PER_YIELD){
                //Thread.yield();   // give another thread a chance to run
                noDelays = 0;
            }
        }

        beforeTime = System.currentTimeMillis();
        /* If frame animation is taking too long, update the game state
           without rendering it, to get the updates/sec nearer to
           the required FPS. */
        int skips = 0;
        while((excess > period) && (skips < MAX_FRAME_SKIPS)) {
            excess -= period;
            gameUpdate();    // update state but don't render
            skips++;
        }
        framesSkipped += skips;
        storeStats();
    }
    printStats();
    System.exit(0);// so window disappears
  } // end of run()


  private void gameUpdate(){
      if(!isPaused && !gameOver){
          int amountAlive = 0;
          for(Slidingzeus oneZeus : this.mrZeus){
            if(oneZeus.getHealth() > 0){//less then 1, dead zeus
              oneZeus.move();//MOVE!
              amountAlive++;
            }
            for(Slidingzeus singleZeus : this.mrZeus){
              if(!oneZeus.equals(singleZeus)){//Ignore self.
                  int[] headpos = oneZeus.GetHeadPosn();
                  if(singleZeus.CollisionXY(headpos[0],headpos[1])){
                      oneZeus.slapHealth(100);
                  }
              }
            }
          }
          if(amountAlive < 2&&Players!=1){//still more then 1 alive?
              gameOver();
          }else if(Players==1&&amountAlive==0){
              gameOver();
          }
      }
      
  }  // end of gameUpdate()

  private void gameOver(){
      highScore="";
      Winner="";
      Winners=0;
      for(int i = 0; i < this.mrZeus.size(); i++){
            if(mrZeus.get(i).getHealth() > 0){
              mrZeus.get(i).Score++;
              Winner = this.Players_Name[i];
              Winners++;
            }
            highScore += Players_Name[i]+" "+mrZeus.get(i).Score+"|";
      }
      gameOver = true;
  }

  private void gameRender(){
    if(dbImage == null){
      dbImage = createImage(PWIDTH, PHEIGHT);
      if (dbImage == null) {
        System.out.println("dbImage is null");
        return;
      }else{
        dbg = dbImage.getGraphics();
      }
    }
    // clear the background
    dbg.setColor(Color.white);
    dbg.fillRect(0, 0, PWIDTH, PHEIGHT);
    dbg.setFont(font);
    //if(!gameOver){
      this.createGrid(dbg);
    //}
    if(debugmode){
        dbg.setColor(Color.blue);
        
        // report frame count & average FPS and UPS at top left
        // dbg.drawString("Frame Count " + frameCount, 10, 25);
        dbg.drawString("Average FPS/UPS: "+df.format(averageFPS)+", "+df.format(averageUPS),20,25);  // was (10,55)
    }
    dbg.setColor(Color.black);
    // draw game elements: the obstacles and the worm
    //obs.draw(dbg);
    //fred.draw(dbg);
    for(Slidingzeus oneZeus : this.mrZeus){
       oneZeus.draw(dbg);
    }
    if(gameOver){
        if(toggle_help)
        dbg.drawImage(img_help,0,PHEIGHT-329,null);
        //for(Slidingzeus oneZeus : this.mrZeus){
        
        //GameMessage(dbg,"Game Over.\n");
        if(Winners > 0){
            GameMessage(dbg,Winner+" Won the game!|"+highScore);
        }else{
            GameMessage(dbg,"DRAW!|"+highScore);
        }
    }
    else if(isPaused){GameMessage(dbg,"PAUSED!");}
    //else{GameMessage(dbg,"NO!?");}
  }  // end of gameRender()

//center screen msg.
private void GameMessage(Graphics g,String msg){
    //g.setFont(font);//WIERD!
    int longest_str = 0;
    String[] stringarray = msg.split("\\|");
    for(int i = 0; i < stringarray.length; i++){
        if(longest_str < metrics.stringWidth(stringarray[i])){
            longest_str = metrics.stringWidth(stringarray[i]);
        }
    }
    int x = (PWIDTH - longest_str)/2;
    int height = metrics.getHeight();
    int y = (PHEIGHT - height)/2;
    g.setColor(Color.yellow);
    g.fillRect(x-5, y-20,longest_str+10, 10+(height*stringarray.length));
    g.setColor(Color.black);
    for(int i = 0; i < stringarray.length; i++){
        g.drawString(stringarray[i], x, y+(i*height));
    }
    
}

  private void paintScreen(){
  // use active rendering to put the buffered image on-screen
    Graphics g;   
    try {
      g = this.getGraphics();

      if((g != null) && (dbImage != null)){
        g.drawImage(dbImage, 0, 0, null);
      }
      Toolkit.getDefaultToolkit().sync();  // sync the display on some systems
      g.dispose();
    }
    catch (Exception e)
    { System.out.println("Graphics error: " + e);  }
  } // end of paintScreen()


  private void storeStats()
  /* The statistics:
       - the summed periods for all the iterations in this interval
         (period is the amount of time a single frame iteration should take),
         the actual elapsed time in this interval,
         the error between these two numbers;

       - the total frame count, which is the total number of calls to run();

       - the frames skipped in this interval, the total number of frames
         skipped. A frame skip is a game update without a corresponding render;

       - the FPS (frames/sec) and UPS (updates/sec) for this interval,
         the average FPS & UPS over the last NUM_FPSs intervals.

     The data is collected every MAX_STATS_INTERVAL  (1 sec).
  */
  {
    frameCount++;
    statsInterval += period;

    if (statsInterval >= MAX_STATS_INTERVAL) {     // record stats every MAX_STATS_INTERVAL
      if(isPaused==false){
        timeNow = System.currentTimeMillis();
      }
      timeSpentInGame = (int) ((timeNow - gameStartTime)/1000L);  // ms --> secs
      if(!gameOver)
      wcTop.setTimeSpent( timeSpentInGame );

      long realElapsedTime = timeNow - prevStatsTime;   // time since last stats collection
      totalElapsedTime += realElapsedTime;

      //double timingError = ((double)(realElapsedTime - statsInterval) / statsInterval) * 100.0;

      totalFramesSkipped += framesSkipped;

      double actualFPS = 0;     // calculate the latest FPS and UPS
      double actualUPS = 0;
      if (totalElapsedTime > 0) {
        actualFPS = (((double)frameCount / totalElapsedTime) * 1000L);
        actualUPS = (((double)(frameCount + totalFramesSkipped) / totalElapsedTime)
                                                             * 1000L);
      }

      // store the latest FPS and UPS
      fpsStore[(int)statsCount%NUM_FPS] = actualFPS;
      upsStore[(int)statsCount%NUM_FPS] = actualUPS;
      statsCount = statsCount+1;

      double totalFPS = 0.0;     // total the stored FPSs and UPSs
      double totalUPS = 0.0;
      for(int i=0; i < NUM_FPS; i++){
        totalFPS += fpsStore[i];
        totalUPS += upsStore[i];
      }

      if(statsCount < NUM_FPS){ // obtain the average FPS and UPS
        averageFPS = totalFPS/statsCount;
        averageUPS = totalUPS/statsCount;
      }else{
        averageFPS = totalFPS/NUM_FPS;
        averageUPS = totalUPS/NUM_FPS;
      }
/*
      System.out.println(timedf.format( (double) statsInterval/1000L) + " " +
                    timedf.format((double) realElapsedTime/1000L) + "s " +
			        df.format(timingError) + "% " +
                    frameCount + "c " +
                    framesSkipped + "/" + totalFramesSkipped + " skip; " +
                    df.format(actualFPS) + " " + df.format(averageFPS) + " afps; " +
                    df.format(actualUPS) + " " + df.format(averageUPS) + " aups" );
*/
      framesSkipped = 0;
      prevStatsTime = timeNow;
      statsInterval = 0L;   // reset
    }
  }  // end of storeStats()

  private void createGrid(Graphics g){
   //draw grid
     g.setColor(Color.gray);
     for(int i=0; i < PHEIGHT; i+=20){
       g.drawLine(0,i,PWIDTH,i);
     }
     for(int i=0; i < PWIDTH; i+=20){
       g.drawLine(i,0,i,PHEIGHT);
     }
  }

  private void printStats(){
    System.out.println("Frame Count/Loss: " + frameCount + " / " + totalFramesSkipped);
    System.out.println("Average FPS: " + df.format(averageFPS));
    System.out.println("Average UPS: " + df.format(averageUPS));
    System.out.println("Time Spent: " + timeSpentInGame + " secs");
    //System.out.println("Boxes used: " + obs.getNumObstacles());
  }  // end of printStats()

}  // end of ZeusPanel class

