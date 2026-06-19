package project.analytic;

import org.ejml.simple.SimpleMatrix;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static project.Utility.*;
import static project.Values.*;

public class StochasticMethod {

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

    static void createAlphaChart(double[] alphas, int kLow, String filename) {
        XYSeries series = new XYSeries(alpha_char + "max(K)");
        for (int i = 0; i < alphas.length; i++) {
            series.add(kLow + i, alphas[i]);
        }
        XYSeriesCollection collection = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot(alpha_char + "max(K)", "K", alpha_char + "max", collection);
        XYPlot plot = chart.getXYPlot();
        saveWhiteChart(filename, chart, plot);
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
        for (int i = 0; i < usage.getNumRows(); i++) {
            if (usage.get(i) < 1.01 && usage.get(i) > 0.99) J.set(i, Double.POSITIVE_INFINITY);
            else J.set(i, usage.get(i) / (1 - usage.get(i)));
        }
        return J;
    }

    static SimpleMatrix getResponseTime(SimpleMatrix X, SimpleMatrix J) {
        SimpleMatrix T = new SimpleMatrix(J.getNumRows(), J.getNumCols());
        for (int i = 0; i < J.getNumRows(); i++) {
            if (Double.isInfinite(J.get(i))) T.set(i, Double.POSITIVE_INFINITY);
            else T.set(i, J.get(i) / X.get(i));
        }
        return T;

    }

    static double getSystemResponseTime(SimpleMatrix jobs, double alphaMax, double r) {
        return jobs.elementSum() / (alphaMax * r);
    }

    static void addMatrixToStringBuilder(StringBuilder sb, SimpleMatrix sm, int size) {
        for (int i = 0; i < size; i++) {
            if (i < sm.getNumElements()) {
                sb.append(String.format("%.4f", sm.get(i))).append(',');
            } else sb.append("0.0000").append(',');
        }
    }

    static void saveChartMultiple(List<SimpleMatrix> values, double r, int kLow, String title, String xLabel, String yLabel, String filename) {
        XYSeries Cpu = new XYSeries("CPU");
        XYSeries D1 = new XYSeries("SysDisk 1");
        XYSeries D2 = new XYSeries("SysDisk 2");
        XYSeries D3 = new XYSeries("SysDisk 3");
        XYSeries Dk = new XYSeries("UserDisk");

        double maxY = 0.0;
        int k = kLow;
        for (SimpleMatrix sm : values) {
            for (int i = 0; i < 5; i++) {
                if (Double.isFinite(sm.get(i))) {
                    maxY = Math.max(maxY, sm.get(i));
                }
            }
            Cpu.add(k, Double.isFinite(sm.get(0)) ? sm.get(0) : 100);
            D1.add(k, Double.isFinite(sm.get(1)) ? sm.get(1) : 100);
            D2.add(k, Double.isFinite(sm.get(2)) ? sm.get(2) : 100);
            D3.add(k, Double.isFinite(sm.get(3)) ? sm.get(3) : 100);
            Dk.add(k, Double.isFinite(sm.get(4)) ? sm.get(4) : 100);
            k++;
        }
        XYSeriesCollection collection = new XYSeriesCollection();
        collection.addSeries(Cpu);
        collection.addSeries(D1);
        collection.addSeries(D2);
        collection.addSeries(D3);
        collection.addSeries(Dk);

        JFreeChart chart = ChartFactory.createXYLineChart(title + " r = " + r, xLabel, yLabel, collection);
        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setRange(0.0, maxY * 1.1);
        saveWhiteChart(filename, chart, plot);
    }

    static void saveChartSingle(List<Double> values, double r, int kLow, String title, String xLabel, String yLabel, String filename) {
        XYSeries R = new XYSeries("ResponseTime");
        int k = kLow;
        for (Double d : values) {
            R.add(k, d);
            k++;
        }
        XYSeriesCollection collection = new XYSeriesCollection(R);
        JFreeChart chart = ChartFactory.createXYLineChart(title + " r = " + r, xLabel, yLabel, collection);
        XYPlot plot = chart.getXYPlot();
        saveWhiteChart(filename, chart, plot);

    }

    private static void saveWhiteChart(String filename, JFreeChart chart, XYPlot plot) {
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        try {
            ChartUtilities.saveChartAsPNG(new File("results/" + filename + ".png"), chart, 800, 600);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    static void solveNetworkAndWriteToCSV(List<SimpleMatrix> lambdas, double[] r_values, double[] base_service_times, double[] alpha_max_array, int kLow, int kHigh, double k_service_time, String filename) {
        Path path = Path.of("results/" + filename + ".csv");
        StringBuilder sb = getHeader(kHigh);

        for (double r : r_values) {

            List<SimpleMatrix> UList = new ArrayList<>();
            List<SimpleMatrix> TList = new ArrayList<>();
            List<Double> RList = new ArrayList<>();
            for (int k = kLow; k <= kHigh; k++) {
                double[] service_times = extendServiceTimes(base_service_times, k, k_service_time);
                SimpleMatrix lambda = lambdas.get(k - kLow);
                double alpha_max = alpha_max_array[k - kLow];
                SimpleMatrix X = getThroughput(lambda, r, alpha_max);
                SimpleMatrix U = getUsage(X, service_times);
                SimpleMatrix J = getAvgJobs(U);
                SimpleMatrix T = getResponseTime(X, J);
                double R = getSystemResponseTime(J, alpha_max, r);
                sb.append(k).append(',').append(r).append(',');
                addMatrixToStringBuilder(sb, U, base_service_times.length + kHigh);
                addMatrixToStringBuilder(sb, X, base_service_times.length + kHigh);
                addMatrixToStringBuilder(sb, J, base_service_times.length + kHigh);
                sb.append(String.format("%.4f", R));
                sb.append('\n');

                UList.add(U);
                TList.add(T);
                RList.add(R);

            }
            saveChartMultiple(UList, r, kLow, "Iskoricenje", "K", "U", "usage_chart_r_" + r);
            saveChartMultiple(TList, r, kLow, "Vreme odziva", "K", "T[s]", "response_chart_r_" + r);
            saveChartSingle(RList, r, kLow, "Vreme odziva sistema", "K", "R[s]", "sys_response_chart_r_" + r);
        }
        try {
            Files.writeString(path, sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    public static void main(String[] args) {

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
        solveNetworkAndWriteToCSV(lambdas, r_values, base_service_times, alphaMaxArray, kLow, kHigh, K_SERVICE_TIME, "rezultati_analiticki");
    }

}
