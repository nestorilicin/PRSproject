package project.simulation;

import org.ejml.simple.SimpleMatrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static project.Utility.*;
import static project.Values.*;

public class Simulation {
    private final int k;
    private final double r;
    private final List<Double> UList = new ArrayList<>();
    private final List<Double> XList = new ArrayList<>();
    private final List<Double> JList = new ArrayList<>();
    private final double T;

    public Simulation(int k, double r, int simulationTime, double amax) {
        this.k = k;
        this.r = r;
        SimpleMatrix P = getP(k);
        //P.print("%.4f");
        double[] service_times = extendServiceTimes(base_service_times, k, K_SERVICE_TIME);
        double alpha = r * amax;
        List<Server> servers = new ArrayList<>();
        for (int i = 0; i < P.getNumRows(); i++) {
            servers.add(new Server(service_times[i], P.getRow(i)));
        }
        for (Server s : servers) {
            s.connectNeighbours(servers);
        }
        PriorityQueue<Event> events = new PriorityQueue<>();
        PoissonProcess inputFlow = new PoissonProcess(alpha);
        events.add(new Event(inputFlow.nextEvent(), EventType.IN, null));
        List<Double> jobFinishedTimeList = new ArrayList<>();
        double currentTime = 0.0;
        while (currentTime < simulationTime && !events.isEmpty()) {
            Event event = events.poll();
            currentTime = event.time();
            if (event.type() == EventType.IN) {
                servers.getFirst().addJob(currentTime, currentTime, events);
                events.add(new Event(currentTime + inputFlow.nextEvent(), EventType.IN, null));
            } else {
                event.server().doneJob(currentTime, events, jobFinishedTimeList);
            }
        }
        for (Server server : servers) {
            XList.add(server.getX(currentTime));
            JList.add(server.getJ(currentTime));
            UList.add(server.getU(currentTime));
        }
        double sum = 0.0;
        for (double d : jobFinishedTimeList) {
            sum += d;
        }
        T = sum / jobFinishedTimeList.size();
    }

    public int getK() {
        return k;
    }

    public double getR() {
        return r;
    }

    public List<Double> getUList() {
        return UList;
    }

    public List<Double> getXList() {
        return XList;
    }

    public List<Double> getJList() {
        return JList;
    }

    public double getT() {
        return T;
    }


    static void addSimulationToStringBuilder(StringBuilder sb, int k, double r, List<Double> UList, List<Double> XList, List<Double> JList, double T, int size) {
        sb.append(k).append(',').append(r).append(',');
        for (int i = 0; i < size; i++) {
            if (i < UList.size()) {
                sb.append(String.format("%.4f", UList.get(i))).append(',');
            } else sb.append("0.0000").append(',');
        }
        for (int i = 0; i < size; i++) {
            if (i < XList.size()) {
                sb.append(String.format("%.4f", XList.get(i))).append(',');
            } else sb.append("0.0000").append(',');
        }
        for (int i = 0; i < size; i++) {
            if (i < JList.size()) {
                sb.append(String.format("%.4f", JList.get(i))).append(',');
            } else sb.append("0.0000").append(',');
        }
        sb.append(String.format("%.4f", T)).append('\n');
    }

    public static void singleSimulationToCsv(int kLow, int kHigh, double[] r_values, int simulationTime, double[] amaxArray, String filename) {
        Path path = Path.of("results/" + filename + ".csv");
        StringBuilder sb = getHeader(kHigh);
        int size = base_service_times.length + kHigh;
        for (double r : r_values) {
            for (int k = kLow; k <= kHigh; k++) {
                Simulation sim = new Simulation(k, r, simulationTime, amaxArray[k - kLow]);
                addSimulationToStringBuilder(sb, sim.getK(), sim.getR(), sim.getUList(), sim.getXList(), sim.getJList(), sim.getT(), size);
            }
        }
        try {
            Files.writeString(path, sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void averageMultipleSimultaionToCsv(int kLow, int kHigh, double[] r_values, int simulationTime, double[] amaxArray, String filename, int count) {
        Path path = Path.of("results/" + filename + ".csv");
        StringBuilder sb = getHeader(kHigh);
        int size = base_service_times.length + kHigh;
        for (double r : r_values) {
            for (int k = kLow; k <= kHigh; k++) {
                int arraysSize = size - kHigh + k;
                double[] sumU = new double[arraysSize];
                double[] sumX = new double[arraysSize];
                double[] sumJ = new double[arraysSize];
                double sumT = 0.0;
                System.out.println("r = " + r + " k = " + k);
                for (int i = 0; i < count; i++) {
                    System.out.println("Simulacija #" + i);
                    Simulation sim = new Simulation(k, r, simulationTime, amaxArray[k - kLow]);
                    List<Double> uList = sim.getUList();
                    List<Double> xList = sim.getXList();
                    List<Double> jList = sim.getJList();
                    for (int j = 0; j < uList.size(); j++) {
                        sumU[j] += uList.get(j);
                        sumX[j] += xList.get(j);
                        sumJ[j] += jList.get(j);
                    }
                    sumT += sim.getT();
                }
                List<Double> avgUList = new ArrayList<>();
                List<Double> avgXList = new ArrayList<>();
                List<Double> avgJList = new ArrayList<>();

                for (int j = 0; j < sumU.length; j++) {
                    avgUList.add(sumU[j] / count);
                    avgXList.add(sumX[j] / count);
                    avgJList.add(sumJ[j] / count);
                }

                double avgT = sumT / count;
                addSimulationToStringBuilder(sb, k, r, avgUList, avgXList, avgJList, avgT, size);
            }
        }
        try {
            Files.writeString(path, sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        int simulationTimeMinutes = args.length > 0 ? Integer.parseInt(args[0]) : 30;
        int simulationTimeSeconds = simulationTimeMinutes * 60; // in seconds
        int count = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        singleSimulationToCsv(kLow, kHigh, r_values, simulationTimeSeconds, amaxArray, "rezultati_simulacija");
        System.out.println("Startovanje " + count + " simulacija za svaki par k i r");
        long startTime =  System.nanoTime();
        averageMultipleSimultaionToCsv(kLow, kHigh, r_values, simulationTimeSeconds, amaxArray, "rezultati_simulacija_usrednjeno", count);
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;
        long durationS = durationMs / 1000;
        System.out.println("Vreme izvrsavanja: " + durationS + " sekundi");
    }

}