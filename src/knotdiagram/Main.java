package knotdiagram;

import georegression.struct.point.Point2D_I32;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageAccessException;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;



public class Main {
	
	// The threshold for use in comparing the angles of endpoints. 0-1 where 1 is strictest.
	/* initial is the first threshold match it looks for in a given range around the endpoints,
	 * if none if found, the algorithm will lower the threshold and look again in that range, until
	 * the angle threshold is lowered to minThresh, and then it will reset the angle threshold to initial,
	 * increase the range around the point it will search, and try again
	 * */
	static double initAngleThresh = 1;
	static double minThresh = .5;

	
	public static void main(String[] args) {
		
		
		//Open a JFileChooser to find the image file
		JFileChooser chooser= new JFileChooser("C:\\Users\\Dale\\Desktop\\Classes 2012-13\\Knot Project\\Knots");

		
		int choice = chooser.showOpenDialog(null);

		if (choice != JFileChooser.APPROVE_OPTION) return;

		File file = chooser.getSelectedFile();
		
		//Make that image file a BufferedImage
		BufferedImage image = UtilImageIO.loadImage(file.getAbsolutePath());
		
		
		//Make a grayscale image from that BufferedImage
		ImageUInt8 gray = ConvertBufferedImage.convertFrom(image,(ImageUInt8)null);
		 
		//Binary
		ImageUInt8 binary = new ImageUInt8(gray.width, gray.height);
		double mean = GPixelMath.sum(gray)/(gray.width*gray.height);
		binary = ThresholdImageOps.threshold(gray, binary, (int)mean, true);
		BufferedImage visualBinary = VisualizeBinaryData.renderBinary(binary,null);
		
		//Erode
		ImageUInt8 eroded = new ImageUInt8(binary.width, binary.height);
		BinaryImageOps.erode4(binary,eroded);
		BinaryImageOps.removePointNoise(eroded, eroded);
		BufferedImage visualEroded = VisualizeBinaryData.renderBinary(eroded, null);
		
		//Thin
		ImageUInt8 thinned = thin(eroded);
		BufferedImage visualThinned = VisualizeBinaryData.renderBinary(thinned,null);
		
		//Blob and label the Thinned
		ImageSInt32 labeledBlobs = new ImageSInt32(thinned.width, thinned.height);
		int numBlobs = BinaryImageOps.labelBlobs8(thinned,labeledBlobs);
		FastQueue<Point2D_I32> Label = new FastQueue<Point2D_I32>(3,Point2D_I32.class, true);
		List<List<Point2D_I32>> labelsList = BinaryImageOps.labelToClusters(labeledBlobs, numBlobs, Label);
		BufferedImage visualBlobs = VisualizeBinaryData.renderLabeled(labeledBlobs, numBlobs, null);
		 
		// Show the results
		List<List<Point2D_I32>> orderedList = order(labelsList, labeledBlobs);		
		List<Segment> contSegs = connectSegments(orderedList, labeledBlobs);
		visualizeCont(contSegs, visualBlobs.getWidth(), visualBlobs.getHeight(), "SequentialGradient");
		visualize(orderedList, visualBlobs.getWidth(), visualBlobs.getHeight(), "PostOrderGradient");
		visualize(labelsList, visualBlobs.getWidth(), visualBlobs.getHeight(), "PreOrderGradient");
		ShowImages.showWindow(visualBlobs, "Thinned Blobs");
		save(visualBlobs, "Blobbed");
		ShowImages.showWindow(visualThinned, "Thinned");
		save(visualThinned, "Thinned");
		ShowImages.showWindow(visualEroded, "Eroded");
		save(visualEroded, "Eroded");
		ShowImages.showWindow(visualBinary, "The Binary");
		save(visualBinary, "Binary");
		ShowImages.showWindow(gray, "The Grayscale");
		ShowImages.showWindow(image, "The Original Image");
		writeSnapPy(contSegs);
		printCoords(contSegs);

		
	}
	
	//=========================================================================
	// Form outputs
	//=========================================================================
	
	// You'll want to change the path for your own purposes.
	public static void save(BufferedImage image, String name) {
		try {
            String path = "C:\\Users\\Dale\\Desktop\\Classes 2012-13\\Knot Project\\Knots\\ppt\\";
            File fileSave = new File(path + name + ".png");
            ImageIO.write(image, "png", fileSave);
        } catch (IOException e) {}
	}
	
	// You'll want to change the path for your purposes. This method is INCOMPLETE.
	public static void writeSnapPy(List<Segment> contSegs) {
		try{
			  // Create file 
			  FileWriter fstream = new FileWriter("C:\\Users\\Dale\\Desktop\\Classes 2012-13\\Knot Project\\Diagram.txt");
			  BufferedWriter out = new BufferedWriter(fstream);
			  
			  // Start writing the file
			  out.write("% Link Projection");
			  out.newLine();
			  
			  //counterMax = 5 means use every 5th point.
			  int counterMax = 5;
			  int counter = counterMax - 1;
			  int pointsUsed = 0;
			 
			  for (Segment s : contSegs) {
				  for (Point2D_I32 p : s.points) {
					  counter++;
					  if (counter == counterMax) {
						  pointsUsed++;
						  counter = 0;
					  }
				  }
			  }
			  
			  
			  out.write("1");
			  out.newLine();
			  out.write("   " + (pointsUsed-1) + "    " + (pointsUsed-1));
			  out.newLine();
			  out.write(Integer.toString(pointsUsed));
			  out.newLine();
			  
			  for(Segment s : contSegs) {
				  for (Point2D_I32 p : s.points) {
					  counter++;
					  if (counter == counterMax) {
						  out.write(" " + p.x + "  " + p.y);
						  out.newLine();
						  counter = 0;
					  }
				  }
			  }
			  
			  System.out.println(pointsUsed + "   " + pointsUsed);
			  
			  out.write(Integer.toString(pointsUsed));
			  out.newLine();
			  
			  for (int n = 0; n < (pointsUsed-1); n++) {
				  out.write("   " + (n) + "    " + (n+1));
				  out.newLine();
			  }
			  
			  out.write("   " + (pointsUsed - 1) + "     " + "0");
			  out.newLine();
			  
			  //Close the output stream
			  out.close();
			  }catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
			  }
	}
	
	// This simply prints the coordinates of the resulting pixels.
	public static void printCoords(List<Segment> contSegs) {
		
		for (Segment seg : contSegs) {
			
			System.out.println("***NEW KNOT SEGMENT - UNDERPASS***");
	
			for( Point2D_I32 p : seg.points ) {
				
				System.out.println("("+ p.x + " , " + p.y + ")");
				
			}
		}
	}
	
//=============================================================================
	//CONNECT SEGMENTS
//=============================================================================
	
	/** This method connects line segments together by their endpoints
	 * @param orderedList A list of lists of points each ordered endpoint to endpoint
	 * @param image The image of the segments you want to connect. This is only necessary as long as
	 *  you want to visualize the connections being made.
	 * @return A list of Segments in order (first is connected to second is connected to third, etc. 
	 * Last is connected to first
	 */
	public static List<Segment> connectSegments (List<List<Point2D_I32>> orderedList, ImageSInt32 image) {

		//Each line Segment has two endpoints. Each endpoint has a parent segment. This will help us later.
		
		List<Segment> segments = new ArrayList<Segment>();
		List<Segment.Endpoint> endpoints = new ArrayList<Segment.Endpoint>();
		
		//Add the points of every segment to a segment in the list "segments"
		for (List<Point2D_I32> l : orderedList) {
			segments.add(new Segment(l));
		}
		
		// Add the endpoints to a list of endpoints
		for(Segment s : segments) {
			if(s.endpoints.size() != 2) {
				System.out.println("Error: a segment didn't have exaclty two endpoints");
				break;
			}
			endpoints.add(s.endpoints.get(0));
			endpoints.add(s.endpoints.get(1));
		}
		
		//For every endpoint
		for (Segment.Endpoint ep : endpoints) {
			
			//If this endpoint is not connected yet
			if(ep.neighbor == null) {
				
				//Set up variables to keep track of closest other endpoint
				double minDistance = 999999999;
				Segment.Endpoint minNeighbor = null;
				
				int maxDist = 50;
				double angleThresh = initAngleThresh;
				
				// Read the "while" portion to see that these do loops are for
				do {
					do {
						//For every other endpoint
						for (Segment.Endpoint oep : endpoints) {
							//If this other endpoint is not the same as the first endpoint and it isn't yet connected
							// and they don't share the same segment
							if(!oep.equals(ep) && oep.neighbor == null && !oep.parent.equals(ep.parent)) {						
								//Compute it's distance from the first endpoint
								double thisDistance = Math.pow((oep.point.x - ep.point.x), 2) + 
										   		  	  Math.pow((oep.point.y - ep.point.y), 2);
								//If this distance is less than minDistance and the angles are within threshold, update variables
								if (thisDistance < minDistance && 
										dot(ep.point, ep.backOffPoint, oep.point, ep.point) >= angleThresh &&
										dot(oep.point, oep.backOffPoint, ep.point, oep.point) >= angleThresh &&
										thisDistance <= maxDist) {
									minDistance = thisDistance;
									minNeighbor = oep;
								}
							}
						}
						if (minNeighbor == null) {
							angleThresh -= 0.05;
//							System.out.println("angleThresh adjusted to " + angleThresh);
						}
					// As long as the angleThresh isn't too forgiving, keep adjusting it lower to find a match
					}while (minNeighbor == null && angleThresh >= minThresh);
					//But if no match is found from adjusting the angle, try searching a wider area.
					angleThresh = initAngleThresh;
					maxDist += 50;
//					System.out.println("maxDist adjusted to " + maxDist);
					
				} while (minNeighbor == null);
					
				//Set this endpoint's neighbor to be the minimum distance other endpoint and vice versa
				ep.neighbor = minNeighbor;
				minNeighbor.neighbor = ep;
			}
		}
		
		// Now we start adding segments to the final list to be returned by this method.
		List<Segment> contSegs = new ArrayList<Segment>(); //Continuous Segments
		if(segments.isEmpty()) {
			System.out.println ("Error: \"segments\" is empty");
		}
		Segment currentSeg = segments.get(0);
		Segment.Endpoint currentEP = currentSeg.endpoints.get(1);
		int iterations = 0;
		
		// In this loop we crawl through the line adding segments and updating endpoints
		outerloop:
		while(iterations < 1000) {
			// Start by adding the segment we're on
			contSegs.add(currentSeg);
			
			// Then update currentEP to the EP of the next segment
			currentEP = currentEP.neighbor;
			
			// Then update currentSeg to the neighbor's parent (the next segment in the line)
			currentSeg = currentEP.parent;
			
			// This is the exit condition for the whole loop
			for (Segment seg : contSegs) {
				/* If one of the segments in contSegs is the same as the next segment to be added,
				 * then you've added all of the segments and it's time to break out of this whole loop */
				if (seg.endpoints.get(0).point.x == currentSeg.endpoints.get(0).point.x) {
					break outerloop;
				}
			}
			
			//If we got to this next segment from EP[0], we know the next EP is EP[1]
			if (currentEP.point.x == currentSeg.endpoints.get(0).point.x &&
				currentEP.point.y == currentSeg.endpoints.get(0).point.y) {
				currentEP = currentSeg.endpoints.get(1);
			// Otherwise if we got to this next segment from EP[1] we know the next EP is EP[0]
			} else if (currentEP.point.x == currentSeg.endpoints.get(1).point.x &&
					   currentEP.point.y == currentSeg.endpoints.get(1).point.y) {
				currentEP = currentSeg.endpoints.get(0);
				currentSeg.points = reversePointOrder(currentSeg);
			} else {
				System.out.println("This should never get printed");
			}
			
			iterations++;
		}
		
		
		//This just prints out the individual connections. It's for visualizing and debugging only.
		List<Segment.Endpoint> cond = new ArrayList<Segment.Endpoint>();
		//Counter is just for filenames.
		int counter = 0;
		
		for(Segment.Endpoint ep : endpoints) {
			BufferedImage out = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_BGR);
			
			boolean breakhere = false;
			
			for (Segment.Endpoint condep : cond) {
				if (ep == condep){
					breakhere = true;
				}
			}
			
			if (breakhere == true){
				continue;
			}
			
			cond.add(ep.neighbor);
			
			for (List<Point2D_I32> label : orderedList) {
				
				
				for( Point2D_I32 p : label ) {	
					
					out.setRGB(p.x,p.y, Color.white.getRGB());
				}
			}
		
			out.setRGB(ep.point.x,ep.point.y, Color.red.getRGB());
			out.setRGB(ep.point.x + 1,ep.point.y, Color.red.getRGB());
			out.setRGB(ep.point.x - 1,ep.point.y, Color.red.getRGB());
			out.setRGB(ep.point.x,ep.point.y + 1, Color.red.getRGB());
			out.setRGB(ep.point.x,ep.point.y - 1, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x, ep.neighbor.point.y, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x + 1, ep.neighbor.point.y, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x - 1, ep.neighbor.point.y, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x, ep.neighbor.point.y + 1, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x, ep.neighbor.point.y - 1, Color.red.getRGB());
			out.setRGB(ep.point.x + 2,ep.point.y, Color.red.getRGB());
			out.setRGB(ep.point.x - 2,ep.point.y, Color.red.getRGB());
			out.setRGB(ep.point.x,ep.point.y + 2, Color.red.getRGB());
			out.setRGB(ep.point.x,ep.point.y - 2, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x, ep.neighbor.point.y, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x + 2, ep.neighbor.point.y, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x - 2, ep.neighbor.point.y, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x, ep.neighbor.point.y + 2, Color.red.getRGB());
			out.setRGB(ep.neighbor.point.x, ep.neighbor.point.y - 2, Color.red.getRGB());
			ShowImages.showWindow(out,"Endpoints");
			counter++;
			save(out, "EPs"+counter);
		}
		
		return contSegs;
		
	}
	
	
	// Helper method to reverse the order of points in a line segment
	public static List<Point2D_I32> reversePointOrder (Segment inputSeg) {
		
		List<Point2D_I32> reversedPoints = new ArrayList<Point2D_I32>();
		
		int numberOfPoints = inputSeg.points.size();
		
		// Count down from the last point to the first point in the list
		for (int i = (numberOfPoints - 1); i >= 0; i--) {
			reversedPoints.add(inputSeg.points.get(i));
		}
		
		
		return reversedPoints;
	}
	
	// Calculates the dot product of two vectors
	public static double dot(Point2D_I32 u1, Point2D_I32 u2, Point2D_I32 v1, Point2D_I32 v2) {
		
		
		// 0 = X and 1 = Y
		double[] U = {u1.x - u2.x, u1.y - u2.y}; 
		double[] V = {v1.x - v2.x, v1.y - v2.y};
		
		// I have to unitize these vectors
		double ULength = Math.sqrt( (U[0] * U[0]) + (U[1] * U[1]) );
		double VLength = Math.sqrt( (V[0] * V[0]) + (V[1] * V[1]) );
		U[0] = U[0] / ULength;
		U[1] = U[1] / ULength;
		V[0] = V[0] / VLength;
		V[1] = V[1] / VLength;
		
		
		//Dot product is dot(U,V) = (u1 * v1) + (u2 * v2)
		double dot = (U[0] * V[0]) + (U[1] * V[1]);
		
		return dot;
	}
	
	// Orders points in a line segment from endpoint to endpoint
	public static List<List<Point2D_I32>> order(List<List<Point2D_I32>> labelsList, ImageSInt32 image){
		
		List<List<Point2D_I32>> ordered = new ArrayList<List<Point2D_I32>>();
		
		for (List<Point2D_I32> label : labelsList) {
			
			List<Point2D_I32> orderedLabel = new ArrayList<Point2D_I32>();
		
			// Find an endpoint
			Point2D_I32 endpoint1 = null;
			for( Point2D_I32 p : label ) {
				if (neighbors(p.x, p.y, image) == 1) {
					endpoint1 = p;
					break;
				}
			}
			
			if (endpoint1 != null) {
				orderedLabel = scanLine(image, endpoint1, orderedLabel);
			}
			
			ordered.add(orderedLabel);
			
		}
		
		return ordered;
	}
	
	// Helper for ordering. Scans through the pixels in the segment, in the right order, adding them to a new list.
	public static List<Point2D_I32> scanLine (ImageSInt32 image, Point2D_I32 p, List<Point2D_I32> orderedLabel) {
		
		//First make this pixel background
		image.set(p.x, p.y, 0);
		int pNeighbors = neighbors(p.x,p.y,image);
		
		//If it has one neighbor it's part of the body of the line
		if( pNeighbors == 1) {
			//Add this point to the list
			orderedLabel.add(p);
			//Recurse over it's only neighbor
			List <Point2D_I32> neighbors = getNeighbors(p.x, p.y, image);
			if (neighbors.size() == 1) {
				Point2D_I32 onlyNeighbor = neighbors.get(0);
				scanLine(image, onlyNeighbor, orderedLabel);
			} else {
				System.out.println("scanLine error. A point had multiple or zero neighbors");
			}
			
		//If it has 0 neighbors it should be the other endpoint
		} else if(pNeighbors == 0) {
			orderedLabel.add(p);
		} else {
			System.out.println("scanLine error. Not 1 or 0 neighbors.");
		}
		
		return orderedLabel;
	}
	
	//=========================================================================
	// VISUALIZATIONS
	//=========================================================================
	
	public static void visualize(List<List<Point2D_I32>> labelsList, int width, int height, String fileName) {
		
		BufferedImage out = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		
		Random rand = new Random();
		for (List<Point2D_I32> label : labelsList) {
			
			int rgb = rand.nextInt() | 0x101010;
			int alpha = (rgb >> 24) & 255;
			int red = (rgb >> 16) & 255;
			int green = (rgb >> 8) & 255;
			int blue = rgb & 255;
			
			for( Point2D_I32 p : label ) {
				
				if(red < 250)  { red   += 1;}
				if(green < 250){ green += 1;}
				if(blue < 250) { blue  += 1;}
				rgb = red;
				rgb = (rgb << 8) + green;
				rgb = (rgb << 8) + blue;
				
				//System.out.println(alpha + " " + red + " " + green + " " + blue);
				out.setRGB(p.x,p.y, rgb);
			}
		}
		
		ShowImages.showWindow(out,fileName);
		if(fileName != null) {
			save(out, fileName);
		}
	}
	
	// This is just a special visualization for continuous segments
	public static void visualizeCont(List<Segment> contSegs, int width, int height, String fileName) {
		
		BufferedImage out = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		
		Random rand = new Random();
		for (Segment seg : contSegs) {
			
			int rgb = rand.nextInt() | 0x101010;
			int alpha = (rgb >> 24) & 255;
			int red = (rgb >> 16) & 255;
			int green = (rgb >> 8) & 255;
			int blue = rgb & 255;
			
			for( Point2D_I32 p : seg.points ) {
				
				if(red < 250)  { red   += 1;}
				if(green < 250){ green += 1;}
				if(blue < 250) { blue  += 1;}
				rgb = red;
				rgb = (rgb << 8) + green;
				rgb = (rgb << 8) + blue;
				
				//System.out.println(alpha + " " + red + " " + green + " " + blue);
				out.setRGB(p.x,p.y, rgb);
			}
		}
		ShowImages.showWindow(out,fileName);
		if(fileName != null) {
			save(out, fileName);
		}
	}
	
	//=========================================================================
	// ZHANG-SUEN THINNING
	//=========================================================================
	
	/** Implements Zhang Suen Thinning, modified so that all foreground pixels have one one or two neighbors
	 * http://www-student.cs.uni-bonn.de/~hanisch/sk/zhangsuen.html
	 * @param image The image to thin
	 * @return The resulting thinned image
	 */
	public static ImageUInt8 thin (ImageUInt8 image) {
		
		//Make "Thinned" a copy of the image
		ImageUInt8 Thinned = new ImageUInt8(image.width, image.height);
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y <image.getHeight(); y++) {
				Thinned.set(x, y, image.get(x,y));
			}
		}
		
		// The overall algorithm
		boolean stop = false;
		while (stop == false){
		
			// Keeps track of whether the sub iterations deleted something this loop
			boolean sub1D = false;
			boolean sub2D = false;
			
			//FIRST SUBITERATION
			//These vectors store the location of pixels to delete
			Vector<Integer> dX = new Vector<Integer>();
			Vector<Integer> dY = new Vector<Integer>();
			//For every pixel
			for (int x = 0; x < Thinned.getWidth(); x++) {
				for (int y = 0; y <Thinned.getHeight(); y++) {
					//If this pixel is foreground
					if( Thinned.get(x, y) == 1) {
						// and If this pixel's neighbor value is 2 or more and 6 or less
						if(2 <= neighbors(x, y, Thinned) && neighbors(x, y, Thinned) <= 6){
							// and If this pixel's connectivity is one
							if (connectivity(x, y, Thinned) == 1){
								int p0;
//								int p1;
								int p2;
//								int p3;
								int p4;
//								int p5;
								int p6;
//								int p7;
								// If a pixel cannot be accessed, set it to background.
								try{ p0 = Thinned.get(x,   y-1); } catch(ImageAccessException e) { p0 = 0; }//Top	
//								try{ p1 = Thinned.get(x+1, y-1); } catch(ImageAccessException e) { p1 = 0; }//TopRight		
								try{ p2 = Thinned.get(x+1, y);   } catch(ImageAccessException e) { p2 = 0; }//Right
//								try{ p3 = Thinned.get(x+1, y+1); } catch(ImageAccessException e) { p3 = 0; }//BottomRight		
								try{ p4 = Thinned.get(x,   y+1); } catch(ImageAccessException e) { p4 = 0; }//Bottom		
//								try{ p5 = Thinned.get(x-1, y+1); } catch(ImageAccessException e) { p5 = 0; }//BottomLeft		
								try{ p6 = Thinned.get(x-1, y);   } catch(ImageAccessException e) { p6 = 0; }//Left		
//								try{ p7 = Thinned.get(x-1, y-1); } catch(ImageAccessException e) { p7 = 0; }//TopLeft
								if( (p0 * p2 * p4 == 0) && (p2 * p4 * p6 == 0) ) {
									dX.add(new Integer(x));
									dY.add(new Integer(y));
								}
							}
						}
						
					}
				}
			}
			//If there is stuff to be deleted?
			if (!dX.isEmpty()) {
				sub1D = true;
				// Delete that stuff
				for (int i = 0; i < dX.size(); i++){
					Thinned.set(dX.get(i),dY.get(i), 0);
				}
			}
			
			//SECOND SUBITERATION
			//These vectors store the location of pixels to delete
			Vector<Integer> dX2 = new Vector<Integer>();
			Vector<Integer> dY2 = new Vector<Integer>();
			//For every pixel
			for (int x = 0; x < Thinned.getWidth(); x++) {
				for (int y = 0; y <Thinned.getHeight(); y++) {
					//If this pixel is foreground
					if( Thinned.get(x, y) == 1) {
						// and If this pixel's neighbor value is 2 or more and 6 or less
						if(2 <= neighbors(x, y, Thinned) && neighbors(x, y, Thinned) <=6){
							// and If this pixel's connectivity is one
							if (connectivity(x, y, Thinned) == 1){
								int p0;
//								int p1;
								int p2;
//								int p3;
								int p4;
//								int p5;
								int p6;
//								int p7;
								// If a pixel cannot be accessed, set it to background.
								try{ p0 = Thinned.get(x,   y-1); } catch(ImageAccessException e) { p0 = 0; }//Top	
//								try{ p1 = Thinned.get(x+1, y-1); } catch(ImageAccessException e) { p1 = 0; }//TopRight		
								try{ p2 = Thinned.get(x+1, y);   } catch(ImageAccessException e) { p2 = 0; }//Right
//								try{ p3 = Thinned.get(x+1, y+1); } catch(ImageAccessException e) { p3 = 0; }//BottomRight		
								try{ p4 = Thinned.get(x,   y+1); } catch(ImageAccessException e) { p4 = 0; }//Bottom		
//								try{ p5 = Thinned.get(x-1, y+1); } catch(ImageAccessException e) { p5 = 0; }//BottomLeft		
								try{ p6 = Thinned.get(x-1, y);   } catch(ImageAccessException e) { p6 = 0; }//Left		
//								try{ p7 = Thinned.get(x-1, y-1); } catch(ImageAccessException e) { p7 = 0; }//TopLeft
								if( (p0 * p2 * p6 == 0) && (p0 * p4 * p6 == 0) ) {
									dX2.add(new Integer(x));
									dY2.add(new Integer(y));
								}
							}
						}
						
					}
				}
			}
			//If there is stuff to be deleted
			if (!dX2.isEmpty()) {
				sub2D = true;
				// Delete that stuff
				for (int i = 0; i < dX2.size(); i++){
					Thinned.set(dX2.get(i),dY2.get(i), 0);
				}
			}
			
			// If nothing was deleted this loop, stop
			if(sub1D==false || sub2D==false) {
				stop = true;
			}
			
		}
		
		//This should ensure that every pixel only has 1 or 2 neighbors
		//For every pixel
		for (int x = 0; x < Thinned.getWidth(); x++) {
			for (int y = 0; y <Thinned.getHeight(); y++) {
				//If this pixel is foreground
				if (Thinned.get(x, y) == 1) {
					//If this pixel has more than 2 neighbors
					if(neighbors(x, y, Thinned) > 2){
						//Scan through each neighbor
						int p0;
						int p1;
						int p2;
						int p3;
						int p4;
						int p5;
						int p6;
						int p7;
						// If a pixel cannot be accessed, set it to background.
						try{ p0 = Thinned.get(x,   y-1); } catch(ImageAccessException e) { p0 = 0; }//Top	
						try{ p1 = Thinned.get(x+1, y-1); } catch(ImageAccessException e) { p1 = 0; }//TopRight		
						try{ p2 = Thinned.get(x+1, y);   } catch(ImageAccessException e) { p2 = 0; }//Right
						try{ p3 = Thinned.get(x+1, y+1); } catch(ImageAccessException e) { p3 = 0; }//BottomRight		
						try{ p4 = Thinned.get(x,   y+1); } catch(ImageAccessException e) { p4 = 0; }//Bottom		
						try{ p5 = Thinned.get(x-1, y+1); } catch(ImageAccessException e) { p5 = 0; }//BottomLeft		
						try{ p6 = Thinned.get(x-1, y);   } catch(ImageAccessException e) { p6 = 0; }//Left		
						try{ p7 = Thinned.get(x-1, y-1); } catch(ImageAccessException e) { p7 = 0; }//TopLeft
						
						
						//If the top pixel is foreground
						if (p0 == 1){
							//If one of it's neighbors is 1 also foreground
							if (p7 == 1 || p1 == 1) {
								//Delete it
								Thinned.set(x, y-1, 0);
							}
						}
						
						//If the right pixel is foreground
						if (p2 == 1){
							//If one of it's neighbors is 1 also foreground
							if (p1 == 1 || p3 == 1) {
								//Delete it
								Thinned.set(x+1, y, 0);
							}
						}
						
						//If the bottom pixel is foreground
						if (p4 == 1){
							//If one of it's neighbors is 1 also foreground
							if (p3 == 1 || p5 == 1) {
								//Delete it
								Thinned.set(x, y+1, 0);
							}
						}
						
						//If the left pixel is foreground
						if (p6 == 1){
							//If one of it's neighbors is 1 also foreground
							if (p5 == 1 || p7 == 1) {
								//Delete it
								Thinned.set(x-1, y, 0);
							}
						}
					}
				}
			}
		}
		
		
		return Thinned;
	}
	

	// Determines connectivity for use in the Thinning algorithm
	public static int connectivity(int pX, int pY, ImageUInt8 image) {
		
		int Cn = 0;
		
		int p0;
		int p1;
		int p2;
		int p3;
		int p4;
		int p5;
		int p6;
		int p7;
		// If a pixel cannot be accessed, set it to background.
		try{ p0 = image.get(pX,   pY-1); } catch(ImageAccessException e) { p0 = 0; }//Top	
		try{ p1 = image.get(pX+1, pY-1); } catch(ImageAccessException e) { p1 = 0; }//TopRight		
		try{ p2 = image.get(pX+1, pY);   } catch(ImageAccessException e) { p2 = 0; }//Right
		try{ p3 = image.get(pX+1, pY+1); } catch(ImageAccessException e) { p3 = 0; }//BottomRight		
		try{ p4 = image.get(pX,   pY+1); } catch(ImageAccessException e) { p4 = 0; }//Bottom		
		try{ p5 = image.get(pX-1, pY+1); } catch(ImageAccessException e) { p5 = 0; }//BottomLeft		
		try{ p6 = image.get(pX-1, pY);   } catch(ImageAccessException e) { p6 = 0; }//Left		
		try{ p7 = image.get(pX-1, pY-1); } catch(ImageAccessException e) { p7 = 0; }//TopLeft
		int p8 = p0;//To loop a full circle
		int[] p = {p0,p1,p2,p3,p4,p5,p6,p7,p8};//Array of all of the points to loop through
		
		//See the neighboring pixels
//		System.out.println(p[7] + " " + p[0] + " " + p[1]);
//		System.out.println(p[6] + " " + "X" + " " + p[2]);
//		System.out.println(p[5] + " " + p[4] + " " + p[3]);
		
		for (int i = 0; i < 8; i++) {
			if(p[i]==0 && p[i+1]==1) {
				Cn++;
			}
		}
		
		return Cn;

	}
	
	// Determines the number of foreground neighbors a pixel has for use in the Thinning algorithm
	public static int neighbors (int pX, int pY, ImageUInt8 image) {
	
		int Nn = 0;
		
		try { if (image.get(pX,   pY-1) == 1) {Nn++;}} catch (ImageAccessException e){}//Top
		
		try { if (image.get(pX+1, pY-1) == 1) {Nn++;}} catch (ImageAccessException e){}//TopRight
		
		try { if (image.get(pX+1, pY)   == 1) {Nn++;}} catch (ImageAccessException e){}//Right
		
		try { if (image.get(pX+1, pY+1) == 1) {Nn++;}} catch (ImageAccessException e){}//BottomRight
		
		try { if (image.get(pX,   pY+1) == 1) {Nn++;}} catch (ImageAccessException e){}//Bottom
		
		try { if (image.get(pX-1, pY+1) == 1) {Nn++;}} catch (ImageAccessException e){}//BottomLeft
		
		try { if (image.get(pX-1, pY)   == 1) {Nn++;}} catch (ImageAccessException e){}//Left
		
		try { if (image.get(pX-1, pY-1) == 1) {Nn++;}} catch (ImageAccessException e){}//TopLeft
		
		return Nn;
	}

	// Neighbors when the image file is an SInt32. Needs to be updated to catch imageaccess exceptions.
	public static int neighbors (int pX, int pY, ImageSInt32 image) {
	
		int Nn = 0;
	
		if (image.get(pX,   pY-1) != 0) {Nn++;}//Top
	
		if (image.get(pX+1, pY-1) != 0) {Nn++;}//TopRight
	
		if (image.get(pX+1, pY)   != 0) {Nn++;}//Right
	
		if (image.get(pX+1, pY+1) != 0) {Nn++;}//BottomRight
	
		if (image.get(pX,   pY+1) != 0) {Nn++;}//Bottom
	
		if (image.get(pX-1, pY+1) != 0) {Nn++;}//BottomLeft
	
		if (image.get(pX-1, pY)   != 0) {Nn++;}//Left
	
		if (image.get(pX-1, pY-1) != 0) {Nn++;}//TopLeft
	
	return Nn;
	}
	
	// returns a list of the neighbors connected to the pixel at pX, pY in the image "image".
	public static List<Point2D_I32> getNeighbors (int pX, int pY, ImageSInt32 image) {
	
		List<Point2D_I32> neighbors = new ArrayList<Point2D_I32>();
		
		if (image.get(pX,   pY-1) != 0) {neighbors.add(new Point2D_I32(pX, pY-1));}//Top
		
		if (image.get(pX+1, pY-1) != 0) {neighbors.add(new Point2D_I32(pX+1, pY-1));}//TopRight
	
		if (image.get(pX+1, pY)   != 0) {neighbors.add(new Point2D_I32(pX+1, pY));}//Right
	
		if (image.get(pX+1, pY+1) != 0) {neighbors.add(new Point2D_I32(pX+1, pY+1));}//BottomRight
	
		if (image.get(pX,   pY+1) != 0) {neighbors.add(new Point2D_I32(pX, pY+1));}//Bottom
	
		if (image.get(pX-1, pY+1) != 0) {neighbors.add(new Point2D_I32(pX-1, pY+1));}//BottomLeft
	
		if (image.get(pX-1, pY)   != 0) {neighbors.add(new Point2D_I32(pX-1, pY));}//Left
	
		if (image.get(pX-1, pY-1) != 0) {neighbors.add(new Point2D_I32(pX-1, pY-1));}//TopLeft
		
		return neighbors;
	}
}

	

