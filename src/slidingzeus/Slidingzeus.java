package slidingzeus;
/* Contains the zeus's internal data structure (a circular buffer)
and code for deciding on the position and compass direction
of the next zeus move.
 */

import java.awt.*;
import java.awt.geom.*;

//This is the character, we call him mr Zeus!
public class Slidingzeus {
    // if zero then mr zeus dead, GAMER OVER!
    private int Health = 100;
    //private int Status = 0;
    public int Score = 0;
    // size and number of dots in a ZEUS!
    private static final int DOTSIZE = 10;
    //private static final int RADIUS = DOTSIZE/2;
    private static final int MAXPOINTS = 10000;
    // compass direction/bearing constants
    private static final int NUM_DIRS = 4;
    private static final int N = 0;// north, etc going clockwise
    private static final int E = 1;
    private static final int S = 2;
    private static final int W = 3;
    //private static final int NE = 4;
    //private static final int SE = 5;
    //private static final int SW = 6;
    //private static final int NW = 7;
    private int currCompass;  // stores the current compass dir/bearing
    private boolean OriginBearing = false; // jack does not aprove.
    public int key_bearing = N;
    // Stores the increments in each of the compass dirs.
    // An increment is added to the old head position to get the
    // new position.
    Point2D.Double incrs[];
    // cells[] stores the dots making up the worm
    // it is treated like a circular buffer
    private Point cells[];
    private int nPoints;
    private int tailPosn, headPosn;   // the tail and head of the buffer
    private int pWidth, pHeight;   // panel dimensions
    private int spawnX;// Spawn location!
    private int spawnY;
    private Color Owncolor;// mrZeus color!

    public Slidingzeus(int pW, int pH, int spawnX, int spawnY, Color color) {
        pWidth = pW;
        pHeight = pH;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.Owncolor = color;
        cells = new Point[MAXPOINTS];   // initialise buffer
        nPoints = 0;
        headPosn = -1;
        tailPosn = -1;

        // increments for each compass dir
        incrs = new Point2D.Double[NUM_DIRS];
        incrs[N] = new Point2D.Double(0.0, -1.0);
        //incrs[NE] = new Point2D.Double(0.7, -0.7);
        incrs[E] = new Point2D.Double(1.0, 0.0);
        //incrs[SE] = new Point2D.Double(0.7, 0.7);
        incrs[S] = new Point2D.Double(0.0, 1.0);
        //incrs[SW] = new Point2D.Double(-0.7, 0.7);
        incrs[W] = new Point2D.Double(-1.0, 0.0);
        //incrs[NW] = new Point2D.Double(-0.7, -0.7);
    } // end of Worm()

    public void Reset(){
        cells = new Point[MAXPOINTS];   // initialise buffer
        nPoints = 0;
        headPosn = -1;
        tailPosn = -1;
        Health = 100;
        key_bearing = N;
        currCompass = 0;
    }
    public boolean nearHead(int x, int y) {
        // is (x,y) near the worm's head?
        if (nPoints > 0) {
            /*if( (Math.abs( cells[headPosn].x + RADIUS - x) <= DOTSIZE) &&
            (Math.abs( cells[headPosn].y + RADIUS - y) <= DOTSIZE) )
            return true;*/
        }
        return false;
    } // end of nearHead()

    public boolean touchedAt(int x, int y) {
        // is (x,y) near any part of the worm's body?
        int i = tailPosn;
        /*while (i != headPosn) {
        if( (Math.abs( cells[i].x + RADIUS - x) <= RADIUS) &&
        (Math.abs( cells[i].y + RADIUS - y) <= RADIUS) )
        return true;
        i = (i+1) % MAXPOINTS;
        }
         */
        return false;
    }  // end of touchedAt()

    public void move() {
        /* A move causes the addition of a new dot to the front of
        the worm, which becomes its new head. A dot has a position
        and compass direction/bearing, which is derived from the
        position and bearing of the old head.

        move() is complicated by having to deal with 3 cases:
         * when the worm is first created
         * when the worm is growing
         * when the worm is MAXPOINTS long (then the addition
        of a new head must be balanced by the removal of a
        tail dot)
         */
        int prevPosn = headPosn;  // save old head posn while creating new one
        headPosn = (headPosn + 1) % MAXPOINTS;

        if (nPoints == 0) {   // empty array at start
            tailPosn = headPosn;
            currCompass = calcBearing(key_bearing);
            //currCompass = (int)( Math.random()*NUM_DIRS );  // random dir.
            cells[headPosn] = new Point(this.spawnX, this.spawnY); // center pt
            nPoints++;
        } else if (nPoints == MAXPOINTS) {    // array is full
            tailPosn = (tailPosn + 1) % MAXPOINTS;    // forget last tail
            newHead(prevPosn);
        } else {     // still room in cells[]
            newHead(prevPosn);
            nPoints++;
        }
        if(TouchSelf() == true){
            slapHealth(100);//dead zeus!
           if(this.Score!=0){
               this.Score--;
           }
        }
    }  // end of move()

    private void newHead(int prevPosn) {
        /* Create new head position and compass direction/bearing.

        This has two main parts. Initially we try to generate
        a head by varying the old position/bearing. But if
        the new head hits an obstacle, then we shift
        to a second phase.

        In the second phase we try a head which is 90 degrees
        clockwise, 90 degress clockwise, or 180 degrees reversed
        so that the obstacle is avoided. These bearings are
        stored in fixedOffs[].
         */
        //int fixedOffs[] = {-2, 2, -4};  // offsets to avoid an obstacle

        int newBearing = calcBearing(key_bearing);
        Point newPt = nextPoint(prevPosn, newBearing);
        // Get a new position based on a semi-random
        // variation of the current position.
        cells[headPosn] = newPt;     // new head position
        if (OriginBearing == true) {
            currCompass = newBearing;    // new compass direction
            key_bearing = N;
        }
    }  // end of newHead()

    private int calcBearing(int offset) {
        // Use the offset to calculate a new compass bearing based
        // on the current compass direction.
        int turn = currCompass + offset;
        // ensure that turn is between N to NW (0 to 7)
        if (turn >= NUM_DIRS) {
            turn = turn - NUM_DIRS;
        } else if (turn < 0) {
            turn = NUM_DIRS + turn;
        }
        return turn;
    }  // end of calcBearing()

    private Point nextPoint(int prevPosn, int bearing) {
        /* Return the next coordinate based on the previous position
        and a compass bearing.
        Convert the compass bearing into predetermined increments
        (stored in incrs[]). Add the increments multiplied by the
        DOTSIZE to the old head position.
        Deal with wraparound.
         */
        // get the increments for the compass bearing
        Point2D.Double incr = incrs[bearing];

        int newX = cells[prevPosn].x + (int) (DOTSIZE * incr.x);
        int newY = cells[prevPosn].y + (int) (DOTSIZE * incr.y);
        if (Offscreen(newX, newY) == true) {
            //zeus goes offscreen, NOT GOOD!
            slapHealth(999);//dead zeus!
        }
        // modify newX/newY if < 0, or > pWidth/pHeight; use wraparound
        return new Point(newX, newY);
    }  // end of nextPoint()

    private boolean Offscreen(int newX, int newY) {
        if(newX < 0 || newX + DOTSIZE > pWidth || newY < 0 || newY + DOTSIZE > pHeight){
            return true;
        }
        return false;
    }

    public int getHealth() {
        //How is it going mr Zeus!?
        return this.Health;
    }

    public void slapHealth(int slapHealth) {
        if (this.Health < slapHealth) {
            //Too big of a slap, just kill him!
            this.Health = 0;
        } else {
            //Poor mr Zeus!
            this.Health -= slapHealth;
        }
    }
    /*private void addHealth(int addHealth) {
        if (this.Health + addHealth >= 10000) {
            //mr Zeus isn't allowed more health than 10000
            this.Health = 10000;
        } else {
            this.Health += addHealth;
        }
    }*/

    public int[] GetHeadPosn(){
        if(this.Health==0){
            int temp[]={50,50};
            return temp;
        }else{
            int temp[] = {this.cells[headPosn].x, this.cells[headPosn].y};
            return temp;
        }
    }
    public boolean CollisionXY(int posX, int posY) {
        if (nPoints > 0) {
            int i = tailPosn;
            while (i != headPosn) {
                if ((posX == cells[i].x) && (posY == cells[i].y)) {
                    return true;
                }
                i = (i + 1) % MAXPOINTS;
            }
            //if(headPosn!=-1){
            if ((posX == cells[headPosn].x) && (posY == cells[headPosn].y)) {
                return true;
            }
            //}
        }
        return false;
    }

    private boolean TouchSelf() {
        int i = tailPosn;
        while (i != headPosn) {
            if ((cells[headPosn].x == cells[i].x) && (cells[headPosn].y == cells[i].y)) {
                return true;
            }
            i = (i + 1) % MAXPOINTS;
        }
        return false;
    }

    // draw a black worm with a red head
    public void draw(Graphics g) {
        if (nPoints > 0) {
            //g.setColor(Color.black);
            g.setColor(this.Owncolor);
            int i = tailPosn;
            while (i != headPosn) {
                //g.fillOval(cells[i].x, cells[i].y, DOTSIZE, DOTSIZE);
                //g.drawLine(cells[i].x+(DOTSIZE/2), cells[i].y+(DOTSIZE/2),cells[headPosn].x+(DOTSIZE/2), cells[headPosn].y+(DOTSIZE/2));
                if (this.Health == 0) {
                    g.drawRect(cells[i].x, cells[i].y, DOTSIZE, DOTSIZE);
                } else {
                    g.fillRect(cells[i].x, cells[i].y, DOTSIZE, DOTSIZE);
                    //g.drawLine(cells[i].x+(DOTSIZE/2), cells[i].y+(DOTSIZE/2),cells[headPosn].x+(DOTSIZE/2), cells[headPosn].y+(DOTSIZE/2));
                }
                i = (i + 1) % MAXPOINTS;
            }
            //1g.setColor(this.Owncolor);
            //g.drawLine(cells[headPosn].x+(DOTSIZE/2), cells[headPosn].y+(DOTSIZE/2),pWidth/2,pHeight/2);
            //g.fillOval( cells[headPosn].x, cells[headPosn].y, DOTSIZE, DOTSIZE);
            //g.drawRect(cells[headPosn].x, cells[headPosn].y, DOTSIZE, DOTSIZE);
            if(headPosn!=-1)
            if (this.Health == 0) {
                g.drawRect(cells[headPosn].x, cells[headPosn].y, DOTSIZE, DOTSIZE);
            } else {
                g.fillRect(cells[headPosn].x, cells[headPosn].y, DOTSIZE, DOTSIZE);
            }
        }
        if (ZeusPanel.debugmode) {
            g.setColor(Color.black);
            String debugStr = "hp:" + this.getHealth();
            if(headPosn!=-1)
            PlaceLocation(g, debugStr, cells[headPosn].x, cells[headPosn].y - 10, 8 * debugStr.length(), 15);
        }//*/
    }  // end of draw()

    // this has been made so debug or w/e doesn't fall of the screen!
    public void PlaceLocation(Graphics g, String debugStr, int posX, int posY, int width, int height) {
        if(posX + width > pWidth)
            posX = pWidth - width;//rechts
        if(posX < 5)
            posX = 5;//links
        //else posX normaal!
        if(posY >= pHeight - height)
            posY = pHeight - height;//beneden
        if(posY < height)
            posY = height;//boven
        //else posY normaal!
        g.drawString(debugStr, posX, posY);
    }
}  // end of Worm class

