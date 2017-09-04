package com.kardinia.math;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kardinia.imaging.Picture;

public class MandelbrotHelper {

	
    // return number of iterations to check if c = a + ib is in Mandelbrot set
    public static int mandelbrotPoint(Complex z0, int maxIterations) {
        Complex z = z0;
        for (int t = 0; t < maxIterations - 1; t++) {
            if (z.abs() > 2.0) return t;
            z = z.times(z).plus(z0);
        }
        return maxIterations - 1;
    }
    
    public static Color[] readColourMap(String path) throws NumberFormatException, IOException {
        // read in colour map
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        int lineCount = 0;
        List<Color> colours = new ArrayList<Color>();
        while ((line = br.readLine()) != null) {
          String[] values = line.replaceAll("\\r|\\n", "").split(" ");
          if (values.length != 3) {
        	  br.close();
        	  throw new RuntimeException("line " + lineCount + " doecolumns not contain exactly 3 numbers.");
          }
          colours.add(new Color(Integer.parseInt(values[0]), Integer.parseInt(values[1]), Integer.parseInt(values[2])));
        }
        br.close();

        return colours.toArray(new Color[colours.size()]);
    }
    
    public static void display(int width, int height, Color[] colours, int[][] buffer) {
    	Picture pic = new Picture(width, height);
    	for (int i = 0; i < width; i++) {

    		for (int j = 0; j < height; j++) {
    			pic.set(i, height - 1 - j, colours[buffer[i][j]]);
    		}
    	}
    	
        pic.show();
    }
    
    public static void display(int width, int height, Color[] colours, int[] buffer) {
        Picture pic = new Picture(width, height);
    	for (int i = 0; i < width; i++) {

    		for (int j = 0; j < height; j++) {
    			pic.set(i, height - 1 - j, colours[buffer[i * height + j]]);
    		}
    	}
        pic.show();
    }
}
