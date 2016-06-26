import java.util.*;
import java.util.logging.Logger;
import java.io.*;
import java.math.*;

/**
 * Send your busters out into the fog to trap ghosts and bring them home!
 **/
class Player {
	static ArrayList<Entity> myBusters;
	static ArrayList<Entity> otherBusters;
	static ArrayList<Entity> ghosts;
    static int X, Y;
    static Entity myBase, enemyBase;
    static HashMap<Integer, SessionStatus> myBusterStatus = new HashMap<>();
    static HashMap<Integer, SessionStatus> ghostsSeen = new HashMap<>();
    static final int maxX = 16000;
    static final int maxY = 9000;
    static final int stepSize = 800;
    static final int viewDistance = 2200;
    static final int stunDistance = 1760;


    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
//        while (in.hasNext()) System.out.println(in.nextLine());
        int turn = 0;							// keep track of turns
        int bustersPerPlayer = in.nextInt(); // the amount of busters you control
        int ghostCount = in.nextInt(); // the amount of ghosts on the map
        int myTeamId = in.nextInt(); // if this is 0, your base is on the top left of the map, if it is one, on the bottom right
        // Position of my base camp
        if (myTeamId == 0) {
        	myBase    = new Entity(-2, 0, 0, -2, -2, -2, null);
        	enemyBase = new Entity(-2, maxX, maxY, -2, -2, -2, null);
        }
        else {
        	enemyBase = new Entity(-2, 0, 0, -2, -2, -2, null);
        	myBase    = new Entity(-2, maxX, maxY, -2, -2, -2, null);
        }
    	
        // ********** Game Loop *******************************************************************************
        while (true) {
        	
        	// ********** Init ********************************************************************************
        	System.err.println("Start Init");
        	turn++;
        	myBusters = new ArrayList<>();
            otherBusters = new ArrayList<>();
            ghosts = new ArrayList<>();
            int entities = in.nextInt(); // the number of busters and ghosts visible to you
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // buster id or ghost id
                int x = in.nextInt();
                int y = in.nextInt(); // position of this buster / ghost
                int entityType = in.nextInt(); // the team id if it is a buster, -1 if it is a ghost.
                int state = in.nextInt(); // For busters: 0=idle, 1=carrying a ghost.
                int value = in.nextInt(); // For busters: Ghost id being carried. For ghosts: number of busters attempting to trap this ghost.
                if (entityType == -1)
                        ghosts.add(new Entity(entityId, x, y, entityType, state, value, null));
                else if (entityType == myTeamId)
                        myBusters.add(new Entity(entityId, x, y, entityType, state, value, myBusterStatus.get(entityId)));
                else
                        otherBusters.add(new Entity(entityId, x, y, entityType, state, value, null));
                }
            // Init if first time in loop
            if (turn == 1){
            	int partsSize = (maxX + maxY) / (bustersPerPlayer+1);
            	int busterCnt = 1;
            	int toX, toY, sliceSize;
            	for (Entity buster: myBusters) {
            		// divide field in nrBusters+1 slices of equal size
            		sliceSize = partsSize * busterCnt;
            		if (sliceSize < maxY) {
            			myBusterStatus.put(buster.entityId, new SessionStatus(enemyBase.x, sliceSize, "XY"));
            		} else {
            			myBusterStatus.put(buster.entityId, new SessionStatus(maxX-(sliceSize - maxY), enemyBase.y, "XY"));
            		}
            		buster.sessionStatus = myBusterStatus.get(buster.entityId);
            		busterCnt++;
            		buster.sessionStatus.printBusterDirection();
            	}
            }
            initTurn();

        	// ********** Main ********************************************************************************
            for (Entity buster: myBusters) {
            	String c;
//            	System.err.print("buster "+ buster.entityId);
            	buster.sessionStatus.printBusterDirection();
            	
            	// if this buster carries a ghost bring it to my base
//            	System.err.println("Main 1");
            	if (bringGhostToBase(buster)) continue;
            	
                // if there is a ghost in range capture it
//            	System.err.println("Main 2");
            	if (catchGhostIfInRange(buster)) continue;
                
                // if buster sees ghost, move there
//            	System.err.println("Main 3");
            	if (moveToGhost(buster)) continue;
            	
            	// stun opponent
            	if (stunEnemy(buster)) continue;
            	// go somewhere
//            	System.err.println("Main 4");
            	if (goSomewhere(buster)) continue;
            	System.out.println("MOVE 8000 4500");
                
            }
            //for (int i = 0; i < bustersPerPlayer; i++) {
                
                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");

//             System.out.println("MOVE 8000 4500"); // MOVE x y | BUST id | RELEASE
                
                // Catch Ghost if within range
            //}
        }
    }

    // Init per turn
    static void initTurn(){
    	for (Entity e: myBusters){
    		e.sessionStatus.rechargeStun();
    	}
    }
    
    // true: action performed | false: no action performed
    static boolean bringGhostToBase(Entity buster) {
    	
    	if (buster.state != 1) // not carrying a ghost
    		return false;

    	int distanceToBase = distance(buster, myBase);
    	if (distanceToBase <= 1600) {
    		// within range of base
    		System.out.println("RELEASE");
    	} else {
    		// not within in range of base, so move to it
    		System.out.println("MOVE " + myBase.x + " " + myBase.y);
    	}
    	return true;
    }
    
    // true: action performed | false: no action performed
    static boolean catchGhostIfInRange(Entity buster){
        for (Entity ghost: ghosts) {
        	int distanceToGhost = distance(buster, ghost);
/*                if ( (Math.abs(buster.x - ghost.x) >= 900) && (Math.abs(buster.x - ghost.x) <= 1760) 
                		&& (Math.abs(buster.y - ghost.y) >= 900) && (Math.abs(buster.y - ghost.y) <= 1760) ) { */
            if ( (distanceToGhost >= 900) && (distanceToGhost <= 1760) ) { 
            	System.out.println("BUST " + ghost.entityId);
            	return true;
            }
        }
        return false;
    }
    
    // true: action performed | false: no action performed
    static boolean moveToGhost(Entity buster) {
    	for (Entity ghost: ghosts) {
    		// Buster sees a ghost, move to this ghost
    		System.out.println("MOVE " + ghost.x + " " + ghost.y);
    		return true;
    	}
    	return false;
    }
    
    // true: action performed | false: no action performed
    static boolean stunEnemy(Entity buster) {
    	// if recharging buster can't stun
    	if (buster.sessionStatus.isRecharging())
    		return false;
    	for (Entity e: otherBusters) {
    		// if enemy already stunned, skip
    		if (e.state == 2)
    			continue;
    		if (distance(buster, e) < stunDistance) {
    			// Stun enemy
    			cmdSTUN(e.entityId);
    			return true;
    		}
    	}
    	return false;
    }
    
    // true: action performed | false: no action performed
    static boolean goSomewhere(Entity buster) {
    	// XY defines a predefined direction 
    	if (buster.sessionStatus.direction == "XY")
    		if (giveMoveCommand(buster)) { 
//    			System.err.println("Direction - XY");
    			return true;
    		}
    		else{
//    			System.err.println("Direction - Change to RIGHT");
    			buster.sessionStatus.direction = "RIGHT";
    		}
    	if (buster.sessionStatus.direction == "RIGHT")
    		if (buster.x <= (maxX - viewDistance) ) { 
    			System.out.println("MOVE " + maxX + " " + buster.y);
    			return true;
    		}
    		else
    			buster.sessionStatus.direction = "DOWN";
    	if (buster.sessionStatus.direction == "DOWN")
        		if (buster.y <= (maxY - viewDistance) ) { 
        			System.out.println("MOVE " + buster.x + " " + maxY);
        			return true;
        		}
        		else
        			buster.sessionStatus.direction = "LEFT";
    	if (buster.sessionStatus.direction == "LEFT")
        		if (buster.x > viewDistance) { 
        			System.out.println("MOVE  0 " + buster.y);
        			return true;
        		}
        		else
        			buster.sessionStatus.direction = "TOP";
    	if (buster.sessionStatus.direction == "TOP")
        		if (buster.y > viewDistance) { 
        			System.out.println("MOVE " + buster.x + " 0");
        			return true;
        		}
        		else
        			buster.sessionStatus.direction = "RIGHT";
    	System.out.println("MOVE " + maxX + " " + maxY);
        return true;	
    }
    
	// ********** Utility Methods ********************************************************************************

    static int distance(Entity pos1, Entity pos2) {
    	int xDirection = Math.abs(pos1.x - pos2.x);
    	int yDirection = Math.abs(pos1.y - pos2.y);
    	int dist = (int) Math.sqrt(xDirection*xDirection + yDirection*yDirection);
//    	System.err.println("distance: " + dist);
    	return dist;
    }
    
    // true: action performed | false: no action performed
    static boolean giveMoveCommand(Entity buster){
    	if ( (Math.abs(buster.x - buster.sessionStatus.targetX) > viewDistance) 
    			&& (Math.abs(buster.y - buster.sessionStatus.targetY) > viewDistance) ) {
    		// not within stepSize range of target location AND not at target location yet
    		System.out.println("MOVE " + buster.sessionStatus.targetX + " " + buster.sessionStatus.targetY);
    		return true;
    	}
    	return false;
    }
    
    static void cmdSTUN(int enemyId){
    	System.out.println("STUN " + enemyId);
    }
    
    static void cmdMOVE(int x, int y) {
    	System.out.println("MOVE " + x + " " + y);
    }
}

class Entity {
	// fields containing external input
	int entityId, x, y, entityType, state, value;
	SessionStatus sessionStatus;
	
	Entity (int entityId, int x, int y, int entityType, int state, int value, SessionStatus sessionStatus) {
		this.entityId = entityId;
		this.x = x;
		this.y = y;
		this.entityType = entityType;
		this.state = state;
		this.value = value;
		this.sessionStatus = sessionStatus;
	}
}

class SessionStatus {
	int targetX, targetY, stunWaitTime;
	String direction;

	public SessionStatus(int targetX, int targetY, String direction) {
		setDirection(targetX, targetY, direction);
	}

	void setDirection(int targetX, int targetY, String direction) {
		this.targetX = targetX;
		this.targetY = targetY;
		this.direction = direction;
	}
	
	void setStunWaitTime(){stunWaitTime = 20;}
	void rechargeStun(){if (stunWaitTime>0)stunWaitTime--;}
	boolean isRecharging() {return (stunWaitTime > 0);}
	
	void printBusterDirection(){
    	System.err.println("Buster direction - " + direction + " " + targetX + " " + targetY);
	}
}