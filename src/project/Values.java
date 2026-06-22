package project;

import org.ejml.simple.SimpleMatrix;

public final class Values {
    public static final int kLow = 2;
    public static final int kHigh = 5; // k = 2,3,4,5
    public static final double[] r_values = {0.3, 0.55, 0.8, 1};
    public static final double[] base_service_times = {0.004, 0.01, 0.015, 0.015}; // seconds
    public static final double K_PROBABILITY = 0.4;
    public static final double K_SERVICE_TIME = 0.025;
    public static final SimpleMatrix BaseP = new SimpleMatrix(new double[][]{
            {0.15, 0.2, 0.15, 0.1}, 
            {0.35, 0.25, 0, 0}, 
            {0.35, 0, 0.25, 0}, 
            {0.35, 0, 0, 0.25}});
    public static final String alpha_char = "α";
    public static final String lambda_char = "λ";
    public static double[] amaxArray = {80., 120., 160., 160.}; // calculated analyticaly
}