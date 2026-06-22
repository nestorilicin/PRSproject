package project;

import org.ejml.simple.SimpleMatrix;

import static project.Values.BaseP;
import static project.Values.K_PROBABILITY;

public class Utility {
    public static SimpleMatrix getP(int k) {
        SimpleMatrix P = new SimpleMatrix(BaseP.getNumRows() + k, BaseP.getNumCols() + k);
        P.insertIntoThis(0, 0, BaseP);
        SimpleMatrix newColumns = new SimpleMatrix(BaseP.getNumRows(), k);
        newColumns.fill(K_PROBABILITY / k);
        P.insertIntoThis(0, BaseP.getNumCols(), newColumns);
        return P;
    }

    public static SimpleMatrix getA(int k) {
        SimpleMatrix A = new SimpleMatrix(BaseP.getNumRows() + k, 1);
        A.set(0, 0, 1);
        return A;
    }

    public static double[] extendServiceTimes(double[] base_service_times, int k, double k_service_time) {
        double[] service_times = new double[base_service_times.length + k];
        for (int i = 0; i < base_service_times.length + k; i++) {
            if (i < base_service_times.length) {
                service_times[i] = base_service_times[i];
            } else {
                service_times[i] = k_service_time;
            }
        }
        return service_times;
    }

    public static double findAlphaMax(SimpleMatrix lambda, double[] base_service_times, int k, double k_service_time) {
        double[] service_times = extendServiceTimes(base_service_times, k, k_service_time);
        double alphaMax = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < service_times.length; i++) {
            double local = 1 / service_times[i] / lambda.get(i);
            if (local < alphaMax) {
                alphaMax = local;
                index = i;
            }
        }
        String critical = switch (index) {
            case 0 -> "Sp";
            case 1 -> "Sd1";
            case 2 -> "Sd2";
            case 3 -> "Sd3";
            default -> "Sdk";
        };
        System.out.println(k + "\t" + String.format("%.2f", alphaMax) + "\t\t" + critical);
        return alphaMax;
    }
    
    public static StringBuilder getHeader(int kHigh) {
        StringBuilder sb = new StringBuilder("K,r,Up,Ud1,Ud2,Ud3,");
        for (int i = 1; i <= kHigh; i++) {
            sb.append("Uk").append(i).append(",");
        }
        sb.append("Xp,Xd1,Xd2,Xd3,");
        for (int i = 1; i <= kHigh; i++) {
            sb.append("Xk").append(i).append(",");
        }
        sb.append("Jp,Jd1,Jd2,Jd3,");
        for (int i = 1; i <= kHigh; i++) {
            sb.append("Jk").append(i).append(",");
        }
        sb.append("T\n");
        return sb;
    }
}
