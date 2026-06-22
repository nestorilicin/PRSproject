package project.simulation;

import org.ejml.simple.SimpleMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static project.Utility.extendServiceTimes;
import static project.Utility.getP;
import static project.Values.K_SERVICE_TIME;
import static project.Values.base_service_times;

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

    public static void main(String[] args) {
        int k = 3;
        double r = 0.8;
        int simulationTime = 10000;
        double amax = 120.0;
        Simulation s = new Simulation(k, r, simulationTime, amax);
        List<Double> usageList = s.getUList();
        for (Double u : usageList) {
            System.out.println(u);
        }
        System.out.println(s.getT());
        System.out.println(s.getK());
        System.out.println(s.getR());
        System.out.println(s.getXList());
        System.out.println(s.getJList());
        // expected 0.1606
    }

}