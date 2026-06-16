import org.ejml.simple.SimpleMatrix;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StochasticMethod {
    static final String alpha_char = "α";
    static final String lambda_char = "λ";
    static final double K_PROBABILITY = 0.4;
    static final double K_SERVICE_TIME = 0.025;
    static final SimpleMatrix BaseP = new SimpleMatrix(new double[][]{{0.15, 0.2, 0.15, 0.1}, {0.35, 0.25, 0, 0}, {0.35, 0, 0.25, 0}, {0.35, 0, 0, 0.25}});

    static SimpleMatrix getP(int k) {
        SimpleMatrix P = new SimpleMatrix(BaseP.getNumRows() + k, BaseP.getNumCols() + k);
        P.insertIntoThis(0, 0, BaseP);
        SimpleMatrix newColumns = new SimpleMatrix(BaseP.getNumRows(), k);
        newColumns.fill(K_PROBABILITY / k);
        P.insertIntoThis(0, BaseP.getNumCols(), newColumns);
        return P;
    }

    static SimpleMatrix getA(int k) {
        SimpleMatrix A = new SimpleMatrix(BaseP.getNumRows() + k, 1);
        A.set(0, 0, 1);
        return A;
    }

    static List<SimpleMatrix> getLambdas(int kLow, int kHigh) {
        ArrayList<SimpleMatrix> lambdas = new ArrayList<>();
        for (int k = kLow; k <= kHigh; k++) {
            SimpleMatrix A = getA(k);
            SimpleMatrix P = getP(k);
            // lambda = (I - P^T)-1 * A
            SimpleMatrix lambda = SimpleMatrix.identity(P.getNumRows()).minus(P.transpose()).invert().mult(A);
            lambdas.add(lambda);
        }
        return lambdas;
    }

    static void lambdasToCSV(int kLow, List<SimpleMatrix> lambdas, String filename) {
        Path path = Path.of("results/" + filename + ".csv");
        StringBuilder sb = new StringBuilder("K,");
        for (int i = 1; i <= lambdas.getLast().getNumRows(); i++) {
            sb.append(lambda_char).append(i).append('(').append(alpha_char).append(')');
            if (i != lambdas.getLast().getNumRows()) sb.append(',');
            else sb.append('\n');
        }

        int k = kLow;
        for (SimpleMatrix lambda : lambdas) {
            sb.append(k).append(',');
            for (int i = 0; i < lambda.getNumRows(); i++) {
                sb.append(String.format("%.2f", lambda.get(i)));
                sb.append('*').append(alpha_char);
                if (i != lambda.getNumRows() - 1) sb.append(',');
                else sb.append('\n');
            }
            k++;

        }
        try {
            Files.writeString(path, "\uFEFF" + sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    static double[] extendServiceTimes(double[] base_service_times, int k, double k_service_time) {
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

    static double findAlphaMax(SimpleMatrix lambda, double[] base_service_times, int k, double k_service_time) {
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

    static void createAlphaChart(double[] alphas, int kLow, String filename) {
        XYSeries series = new XYSeries(alpha_char + "max(K)");
        for (int i = 0; i < alphas.length; i++) {
            series.add(kLow + i, alphas[i]);
        }
        XYSeriesCollection collection = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot(alpha_char + "max(K)", "K", alpha_char + "max(K)", collection);
        try {
            ChartUtilities.saveChartAsPNG(new File("results/" + filename + ".png"), chart, 800, 600);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static StringBuilder getHeader(int kHigh) {
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

    static SimpleMatrix getUsage(SimpleMatrix X, double[] service_times) {
        return SimpleMatrix.diag(service_times).mult(new SimpleMatrix(X));
    }

    static SimpleMatrix getThroughput(SimpleMatrix lambda, double alphaMax, double r) {
        return new SimpleMatrix(lambda).scale(alphaMax * r);
    }

    static SimpleMatrix getAvgJobs(SimpleMatrix usage) {
        SimpleMatrix J = new SimpleMatrix(usage.getNumRows(), usage.getNumCols());
        for(int i = 0; i < usage.getNumRows(); i++) {
            J.set(i, usage.get(i) / (1 - usage.get(i)));
        }
        return J;
    }

    static double getResponseTime(SimpleMatrix jobs, double alphaMax, double r) {
        return jobs.elementSum() / (alphaMax * r);
    }

    /*static StringBuilder writeRow(StringBuilder sb, ){
        
        return sb;
    }*/
    static void solveNetworkAndWriteToCSV(List<SimpleMatrix> lambdas, double[] r_values, double[] base_service_times, double[] alpha_max_array, int kLow, int kHigh, double k_service_time, String filename) {
        Path path = Path.of("results/" + filename + ".csv");
        StringBuilder sb = getHeader(kHigh);
        for (int k = kLow; k <= kHigh; k++) {
            for (double r : r_values) {
                double[] service_times = extendServiceTimes(base_service_times, k, k_service_time);
                SimpleMatrix lambda = lambdas.get(k - kLow);
                double alpha_max = alpha_max_array[k - kLow];
                SimpleMatrix X = getThroughput(lambda, r, alpha_max);
                SimpleMatrix U = getUsage(X, service_times);
                SimpleMatrix J = getAvgJobs(U);
                double T = getResponseTime(J, alpha_max, r);
                System.out.println(T);
                
            }
        }
        try {
            Files.writeString(path, sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    public static void main(String[] args) {
        int kLow = 2;
        int kHigh = 5; // k = 2,3,4,5
        double[] base_service_times = {0.004, 0.01, 0.015, 0.015}; // seconds

        List<SimpleMatrix> lambdas = getLambdas(kLow, kHigh);
        // 1)
        lambdasToCSV(kLow, lambdas, "protoci_analiticki");
        // 2)
        System.out.println("K\t" + alpha_char + "max\t\t" + "critical");
        double[] alphaMaxArray = new double[lambdas.size()];
        for (SimpleMatrix lambda : lambdas) {
            int index = lambdas.indexOf(lambda);
            alphaMaxArray[index] = findAlphaMax(lambda, base_service_times, kLow + index, K_SERVICE_TIME);
        }
        createAlphaChart(alphaMaxArray, kLow, "alpha_chart");
        // 3)
        double[] r_values = {0.3, 0.55, 0.8, 0.99};
        solveNetworkAndWriteToCSV(lambdas, r_values, base_service_times, alphaMaxArray, kLow, kHigh, K_SERVICE_TIME, "rezultati_analiticki");
    }

}
