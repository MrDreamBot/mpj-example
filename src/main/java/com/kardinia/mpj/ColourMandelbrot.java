package com.kardinia.mpj;

/*************************************************************************
 *  Compilation:  javac ColourMandelbrot.java
 *  Execution:    java Mandelbrot xmid ymid size colourMap.txt
 *  Dependencies: Picture.java
 *
 *  Plots the Mandelbrot set in color.
 *  
 *  % java ColorMandelbrot 0.1015 -.633 0.01
 *
 *  % java ColorMandelbrot -.5 0 2
 *
 *  // increase dwell
 *  % java ColorMandelbrot -0.7615134027775 0.0794865972225 0.0032285925920
 *
 *************************************************************************/

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.kardinia.imaging.Picture;
import com.kardinia.math.Complex;

import mpi.*;

public class ColourMandelbrot {
	

    static final int WIDTH = 1024;
    static final int HEIGHT = 512;

    static final int ITERS = 256;
	static final int MASTER = 0;
	
	static final int DOTS_TAG = 1;

    // return number of iterations to check if c = a + ib is in Mandelbrot set
    public static int mandelbrot(Complex z0, int d) {
        Complex z = z0;
        for (int t = 0; t < d; t++) {
            if (z.abs() > 2.0) return t;
            z = z.times(z).plus(z0);
        }
        return d;
    }

    public static void main(String[] args) throws IOException  {
        
		// initialise and get program parameters
	    String params[] = MPI.Init(args); 
	    int numberOfProcessors = MPI.COMM_WORLD.Size(); 
	    int myId = MPI.COMM_WORLD.Rank();
	    
	    if (myId == MASTER) {
		    System.out.println("parameter count = " + params.length);
			for (int i = 0; i < params.length; i++) {
				System.out.println(params[i]);
			}

		    System.out.println("Number of processors = " + numberOfProcessors);
		    System.out.println("My Rank(Id) = " + myId);
	    }

	    double startTime = MPI.Wtime();
	    double mandelbrotParams[] = new double[3];
        Color[] colours = new Color[ITERS];
	    
	    if (myId == MASTER) {
	    	// master read in mandelbrot parameters and colour map
	        double xc   = Double.parseDouble(params[0]);
	        double yc   = Double.parseDouble(params[1]);
	        double size = Double.parseDouble(params[2]);

	        // read in colour map
	        BufferedReader br = new BufferedReader(new FileReader(params[3]));
	        String line;
	        int lineCount = 0;
	        while ((line = br.readLine()) != null) {
	          String[] values = line.replaceAll("\\r|\\n", "").split(" ");
	          if (values.length != 3) {
	        	  br.close();
	        	  throw new RuntimeException("line " + lineCount + " does not contain exactly 3 numbers.");
	          }
	          colours[lineCount++] = new Color(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2]));
	          if (lineCount >= ITERS) break;
	        }
	        br.close();
	        if (lineCount != ITERS) {
	      	  throw new RuntimeException("Number of lines " + lineCount + " is less than " + ITERS);
	        } 
	        
		    mandelbrotParams[0] = xc;
		    mandelbrotParams[1] = yc;
		    mandelbrotParams[2] = size;
	    }
	    
	    // all processors get mandelbrot parameters
	    MPI.COMM_WORLD.Bcast(mandelbrotParams, 0, 3, MPI.DOUBLE, 0);
        double xc   = mandelbrotParams[0];
        double yc   = mandelbrotParams[1];
        double size = mandelbrotParams[2];
        
        int dots[] = new int[HEIGHT];
        
        // compute Mandelbrot set
        Picture pic = new Picture(WIDTH, HEIGHT);
        for (int i = 0; i < WIDTH; i += numberOfProcessors) {
        	
        	// compute 1 column
            for (int j = 0; j < HEIGHT; j++) {
            	
                double x = xc - size / 2 + size * (i + myId) / WIDTH;
                double y = yc - size / 2 + size *j / HEIGHT;
                Complex z0 = new Complex(x, y);
                // colour is mapped to number of iterations
                dots[j] = mandelbrot(z0, ITERS - 1);
                
            }
            
            if (myId == MASTER) {
            	for (int j = 0; j < numberOfProcessors; j++) {
            		// receive colour values from each process to plot the mandelbrot
            		if (j != MASTER) {
            			MPI.COMM_WORLD.Recv(dots, 0, HEIGHT, MPI.INT, j, DOTS_TAG);
            		}
            		for (int k = 0; k < HEIGHT; k++) {
            			pic.set(i + j, HEIGHT-1-k, colours[dots[k]]);
            		}
            	}
            }
            else {
            	// send colour values to the master for plotting
            	MPI.COMM_WORLD.Send(dots, 0, HEIGHT, MPI.INT, MASTER, DOTS_TAG);
            }
            
        }
        
        if (myId == MASTER) {
	        System.out.printf("Execution time: %10.2f\n",
	        		MPI.Wtime() - startTime);
	        
	        String name = String.format("mandelbrot-%3.2f-%3.2f.png",  Double.parseDouble(params[0]),  Double.parseDouble(params[1]));
	        if (params.length >= 5) {
	        	name = params[4];
	        }
	        pic.save(name);
	        System.out.printf("Generated mandelbrot saved as: " + name);
	        
	        // commented out due to lack of GUI on ODROID-MC1
	        //pic.show();
        }

	    // terminate MPI env
	    MPI.Finalize(); 
    }

}
