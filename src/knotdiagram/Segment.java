package knotdiagram;

import java.util.ArrayList;
import java.util.List;

import georegression.struct.point.Point2D_I32;

public class Segment {
	
	public static int backOff = 5; // How many pixels to back off from an endpoint to determine the angle

	class Endpoint{
		Segment parent;
		Point2D_I32 point;
		Point2D_I32 backOffPoint;
		Endpoint neighbor = null;
		
		//Endpoint constructor
		public Endpoint(Point2D_I32 inPoint, Segment inParent){
			this.point = inPoint;
			this.parent = inParent;
		}
		
		public void setBackOff() {
			//Set the backoffPoint. Start by finding which of it's parents endpoints it is.
			if (this.parent.endpoints.get(0).point.x == this.point.x &&
				this.parent.endpoints.get(0).point.y == this.point.y) {
				//If it's the first endpoint, you have to back off in the positive
				this.backOffPoint = this.parent.points.get(backOff);
				//If it's the last endpoint, you have to back off in the negative
			} else if (this.parent.endpoints.get(1).point.x == this.point.x &&
					   this.parent.endpoints.get(1).point.y == this.point.y) {
				this.backOffPoint = this.parent.points.get((this.parent.points.size() - 1) - backOff);
			
			} else {
				System.out.println("backoffPoint determination error");
			}
		}
	
	}
	
	//Segment variables
	List<Point2D_I32> points;
	List<Endpoint> endpoints = new ArrayList<Endpoint>();
	
	//Segment constructor
	public Segment(List<Point2D_I32> coords){
		this.points = coords;
		endpoints.add(new Endpoint(this.points.get(0), this));// Add the first endpoint
		endpoints.add(new Endpoint(this.points.get(this.points.size() - 1), this));// Add the last endpoint
		endpoints.get(0).setBackOff();
		endpoints.get(1).setBackOff();
		
	}
}
