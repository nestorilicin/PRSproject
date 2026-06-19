package project.simulation;

import java.util.Random;

public class PoissonProcess {
    private final double lambda;
    private final Random random;

    public PoissonProcess(double lambda) {
        this.lambda = lambda;
        this.random = new Random();
    }

    // https://stackoverflow.com/questions/29020652/java-exponential-distribution
    // https://en.wikipedia.org/wiki/Exponential_distribution#Random_variate_generation
    public double nextEvent() {
        return - Math.log(1.0 - random.nextDouble()) / lambda;
    }
}
