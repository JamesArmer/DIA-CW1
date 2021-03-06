package uk.ac.nott.cs.g53dia.agent;

import uk.ac.nott.cs.g53dia.library.*;
import java.util.Random;


/**
 * A simple example LitterAgent
 * 
 * @author Julian Zappala
 */
/*
 * Copyright (c) 2011 Julian Zappala
 * 
 * See the file "license.terms" for information on usage and redistribution of
 * this file, and for a DISCLAIMER OF ALL WARRANTIES.
 */
public class MyLitterAgent extends LitterAgent {
	//used when the agent needs to find a new target as the new target will always be closer than this point
	Point largePoint = new Point(10000, 10000);

	Point nearestRecharge = new Point(0,0);
	Point currentTarget = largePoint;
	Point nearestWasteStation = largePoint;
	Point nearestRecyclingStation = largePoint;

	Boolean walk = false;
	int randomDirection;
	int walkCounter = 0;

	public MyLitterAgent() {
		this(new Random());
	}

	/**
	 * The tanker implementation makes random moves. For reproducibility, it
	 * can share the same random number generator as the environment.
	 * 
	 * @param r
	 *            The random number generator.
	 */
	public MyLitterAgent(Random r) {
		this.r = r;
	}

	/*
	 * The following is a simple demonstration of how to write a tanker. The
	 * code below is very stupid and simply moves the tanker randomly until the
	 * charge agt is half full, at which point it returns to a charge pump.
	 */
	public Action senseAndAct(Cell[][] view, long timestep) {
		if((walk) && (walkCounter > 0)){
			return randomWalk();
		}
		if(walkCounter == 0){
			resetWalk();
		}

		if((getCurrentCell(view) instanceof RechargePoint) && (getChargeLevel() < MAX_CHARGE)){
			return new RechargeAction(); //if on a recharge point then recharge if possible
		}else if((getCurrentCell(view) instanceof WasteStation) && (getWasteLevel() > 0)) {
			return new DisposeAction();  //if on a WasteStation then dispose of waste if possible
		} else if((getCurrentCell(view) instanceof RecyclingStation) && (getRecyclingLevel() > 0)){
			return new DisposeAction(); //if on a RecyclingStation then dispose of recycling if possible
		}else if((getCurrentCell(view) instanceof WasteBin) && (getRecyclingLevel() == 0)) {
			return loadWaste(view); //load waste if possible
		} else if((getCurrentCell(view) instanceof RecyclingBin) && (getWasteLevel() == 0)){
			return loadRecycling(view); //load recycling if possible
		}else if ((getChargeLevel() <= MAX_CHARGE / 3) || ((this.getPosition().distanceTo(nearestRecharge) < VIEW_RANGE / 6) &&
				(getChargeLevel() <= MAX_CHARGE / 1.5)) && !(getCurrentCell(view) instanceof RechargePoint)) {
			localScan(view);
			return new MoveTowardsAction(nearestRecharge); //move to the nearest recharge station
		}else if((getWasteLevel() >= MAX_LITTER*0.9) || ((getWasteLevel() > 0)
				&& (this.getPosition().distanceTo(nearestWasteStation) < VIEW_RANGE / 5))) { //go to waste station
			localScan(view);
			return new MoveTowardsAction(nearestWasteStation);
		} else if((getRecyclingLevel() >= MAX_LITTER*0.9) || ((getRecyclingLevel() > 0)
				&& (this.getPosition().distanceTo(nearestRecyclingStation) < VIEW_RANGE / 5))){ //go to recycling station
			localScan(view);
			return new MoveTowardsAction(nearestRecyclingStation);
		}else { //move towards waste or recycling bin with task or perform a random walk until one is found
			localScan(view);
			if(currentTarget == largePoint){
				walk = true;
				return randomWalk();
			}
			return new MoveTowardsAction(currentTarget);
		}
	}
	//scans the area that the agent can see and updates all the relevant points so that the closest ones are known
	public void localScan(Cell[][] view){
		for(int i=0;i<(VIEW_RANGE*2);i++) {
			for (int j=0;j<(VIEW_RANGE*2);j++) {
				if (view[i][j] instanceof RechargePoint) { //check for recharge
					RechargePoint rp = (RechargePoint) view[i][j];
					if (newPointCloser(nearestRecharge, rp.getPoint())) {
						nearestRecharge = rp.getPoint();
					}
				}
				if(view[i][j] instanceof WasteStation){ //check for waste station
					WasteStation ws = (WasteStation) view[i][j];
					if(newPointCloser(nearestWasteStation, ws.getPoint())){
						nearestWasteStation = ws.getPoint();
					}
				}
				if(view[i][j] instanceof  RecyclingStation){ //check for recycling station
					RecyclingStation rs = (RecyclingStation) view[i][j];
					if(newPointCloser(nearestRecyclingStation, rs.getPoint())){
						nearestRecyclingStation = rs.getPoint();
					}
				}
				if(view[i][j] instanceof WasteBin){ //check for waste bin
					WasteBin wb = (WasteBin) view[i][j];
					WasteTask wt = wb.getTask();
					if((wt != null) && (wt.getRemaining() > wt.MAX_AMOUNT / 5)) {
						Point wPos = wb.getPoint();
						if(newPointCloser(currentTarget, wPos) && (getRecyclingLevel() == 0)){
							currentTarget = wPos;
						}
					}
				}
				if(view[i][j] instanceof RecyclingBin){ //check for recycling bin
					RecyclingBin rb = (RecyclingBin) view[i][j];
					RecyclingTask rt = rb.getTask();
					if((rt != null) && (rt.getRemaining() > rt.MAX_AMOUNT / 5)){
						Point rPos = rb.getPoint();
						if(newPointCloser(currentTarget, rPos) && (getWasteLevel() == 0)){
							currentTarget = rPos;
						}
					}
				}
			}
		}
	}
	//returns true if the new point is closer to the agent than the old one
	public boolean newPointCloser(Point oldPoint, Point newPoint){
		Point agent = getPosition();
		if(agent.distanceTo(oldPoint) > agent.distanceTo(newPoint)){
			return true;
		}
		return false;
	}
	//loads waste if all the correct criteria are met
	public Action loadWaste(Cell[][] view){
		WasteBin wb = (WasteBin) getCurrentCell(view);
		WasteTask wt = wb.getTask();
		if((wt != null) && (getWasteLevel() < MAX_LITTER)){ //check for task and that the agent has space
			currentTarget = largePoint; //remove this bin as the currentTarget
			return new LoadAction(wt);
		}
		return new MoveAction(r.nextInt(7)); //needed to prevent the agent getting stuck in tightly packed environments
	}
	//loads recycling if all the correct criteria are met
	public Action loadRecycling(Cell[][] view){
		RecyclingBin rb = (RecyclingBin) getCurrentCell(view);
		RecyclingTask rt = rb.getTask();
		if((rt != null) && (getRecyclingLevel() < MAX_LITTER)){ //check for task and that the agent has space
			currentTarget = largePoint; //remove this bin as the currentTarget
			return new LoadAction(rt);
		}
		return new MoveAction(r.nextInt(7)); //needed to prevent the agent getting stuck in tightly packed environments
	}
	//executes the random walk and decreases the counter
	public Action randomWalk(){
		walkCounter--;
		return new MoveAction(randomDirection);
	}
	//resets the random walk and changes the random direction and length
	public void resetWalk(){
		walk = false;
		randomDirection = this.r.nextInt(7);
		walkCounter = this.r.nextInt(6)+20;
	}

}
