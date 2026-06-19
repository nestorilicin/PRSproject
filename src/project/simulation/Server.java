package project.simulation;

import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class Server {

    private static final Random rand = new Random();
    private final PoissonProcess working_flow;

    private final List<Double> transitions = new ArrayList<>();
    private final List<Server> neighbours = new ArrayList<>();

    private final Deque<Double> jobs = new ArrayDeque<>();

    private double lastJobUpdate = 0.0;
    private double jobs_number = 0.0;
    private double jobs_has = 0.0;
    private int jobs_done = 0;

    public Server(double processingTime, SimpleMatrix probabilities) {
        this.working_flow = new PoissonProcess(1 / processingTime);
        double sum = probabilities.elementSum();
        if (sum > 1.0) throw new IllegalStateException("Sum of transitions more than 1.0");
        if (sum > 0.0) {
            double current = 0.0;
            for (int i = 0; i < probabilities.getNumElements(); i++) {
                if (probabilities.get(i) == 0.0) {
                    this.transitions.add(0.0);
                } else {
                    current += probabilities.get(i);
                    this.transitions.add(current);
                }
            }
        } // else end: no neighbours

    }

    public void connectNeighbours(List<Server> servers) {
        if (servers.size() != transitions.size()) {
            return; // end: no neighbours
        }
        neighbours.addAll(servers);
    }

    public void addJob(double job, double currentTime, PriorityQueue<Event> events) {
        if (jobs.isEmpty()) {
            events.add(new Event(currentTime + working_flow.nextEvent(), EventType.OUT, this));
        }
        double dt = currentTime - lastJobUpdate;
        lastJobUpdate = currentTime;
        jobs_number += jobs.size() * dt;
        jobs_has += (jobs.isEmpty() ? 0.0 : 1.0) * dt;
        jobs.addLast(job);
    }

    public void doneJob(double currentTime, PriorityQueue<Event> events, List<Double> responseTimes) {
        double dt = currentTime - lastJobUpdate;
        lastJobUpdate = currentTime;
        jobs_number += jobs.size() * dt;
        jobs_has += (jobs.isEmpty() ? 0.0 : 1.0) * dt;
        double job = jobs.removeFirst();
        jobs_done++;
        Server nextServer = null;
        if (!transitions.isEmpty()) {
            double randomNumber = rand.nextDouble();
            for (int i = 0; i < transitions.size(); i++) {
                if (transitions.get(i) >= randomNumber) {
                    nextServer = neighbours.get(i);
                    break;
                }
            }
            if (nextServer != null) {
                nextServer.addJob(job, currentTime, events);
            }
        } else {
            responseTimes.add(currentTime - job);
        }

        if (!jobs.isEmpty() && !(nextServer == this && jobs.size() == 1)) {
            events.add(new Event(currentTime + working_flow.nextEvent(), EventType.OUT, this));
        }
    }

    public double getX(double currentTime) {
        return jobs_done / currentTime;
    }

    public double getJ(double currentTime) {
        return jobs_number / currentTime;
    }

    public double getU(double currentTime) {
        return jobs_has / currentTime;
    }

}