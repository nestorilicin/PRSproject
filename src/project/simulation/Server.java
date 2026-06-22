package project.simulation;

import org.ejml.simple.SimpleMatrix;

import java.util.*;

public class Server {
    private static final Random rand = new Random();
    private final PoissonProcess workingFlow;
    private final List<Double> transitions = new ArrayList<>();
    private final List<Server> neighbours = new ArrayList<>();
    private final Deque<Double> jobs = new ArrayDeque<>();
    private double lastJobTimestamp = 0.0;
    private double numberOfJobs = 0.0;
    private double hasJobsTime = 0.0;
    private int jobsDone = 0;
    private boolean busy = false;

    public Server(double processingTime, SimpleMatrix probabilities) {
        this.workingFlow = new PoissonProcess(1 / processingTime);
        double sum = probabilities.elementSum();
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

    public void addJob(double jobTimestamp, double currentTime, PriorityQueue<Event> events) {
        double timeDiff = currentTime - lastJobTimestamp;
        lastJobTimestamp = currentTime;
        numberOfJobs += jobs.size() * timeDiff;
        hasJobsTime += (jobs.isEmpty() ? 0.0 : 1.0) * timeDiff;
        jobs.addLast(jobTimestamp);
        if (!busy) {
            busy = true;
            events.add(new Event(currentTime + workingFlow.nextEvent(), EventType.OUT, this));
        }
    }

    public void doneJob(double currentTime, PriorityQueue<Event> events, List<Double> responseTimes) {
        double timeDiff = currentTime - lastJobTimestamp;
        lastJobTimestamp = currentTime;
        numberOfJobs += jobs.size() * timeDiff;
        hasJobsTime += (jobs.isEmpty() ? 0.0 : 1.0) * timeDiff;
        double job = jobs.removeFirst();
        jobsDone++;
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
            // job leaves the system
            responseTimes.add(currentTime - job);
        }
        if (jobs.isEmpty()) {
            busy = false;
        } else {
            events.add(new Event(currentTime + workingFlow.nextEvent(), EventType.OUT, this));
        }
    }

    public double getX(double currentTime) {
        return jobsDone / currentTime;
    }

    public double getJ(double currentTime) {
        return numberOfJobs / currentTime;
    }

    public double getU(double currentTime) {
        return hasJobsTime / currentTime;
    }

}