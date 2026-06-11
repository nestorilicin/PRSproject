import org.ejml.simple.SimpleMatrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class StochasticMethod {
    private static final String alpha_char = "\u03B1";
    private static final String lambda_char = "\u03BB";
    static final double K_PROBABILITY = 0.4;
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

    static List<SimpleMatrix> getLambdas(int kLow, int kHigh, double[] r_values) {
        ArrayList<SimpleMatrix> lambdas = new ArrayList<>();
        for (int k = kLow; k <= kHigh; k++) {
            for (double r : r_values) {
                SimpleMatrix A = getA(k);
                SimpleMatrix P = getP(k);
                // lambda = (I - P^T)-1 * A
                SimpleMatrix lambda = SimpleMatrix.identity(P.getNumRows()).minus(P.transpose()).invert().mult(A);
                lambdas.add(lambda.scale(r));
            }
        }
        return lambdas;
    }

    static void lambdasToCSV(int kLow, int kHigh, double[] r_values, List<SimpleMatrix> lambdas, String filename) {
        Path path = Path.of("results/" + filename + ".csv");
        StringBuilder sb = new StringBuilder("K,R,");
        for(int i = 1; i <= lambdas.getLast().getNumRows(); i++) {
            sb.append(lambda_char).append(i).append('(').append(alpha_char).append(')');
            if(i != lambdas.getLast().getNumRows()) sb.append(',');
            else sb.append('\n');
        }
        
        for (int k = kLow; k <= kHigh; k++) {
            for (double r : r_values) {
                SimpleMatrix lambda = lambdas.removeFirst();
                sb.append(k).append(',').append(r).append(',');
                for(int i = 0; i < lambda.getNumRows(); i++){
                    sb.append(String.format("%.2f", lambda.get(i)));
                    sb.append('*').append(alpha_char);
                    if(i != lambda.getNumRows() - 1) sb.append(',');
                    else sb.append('\n');
                }
            }
        }
        try {
            Files.writeString(path, "\uFEFF" + sb, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

    }

    public static void main(String[] args) {
        double[] r_values = {0.3, 0.55, 0.8, 1};
        int kLow = 2;
        int kHigh = 5; // k = 2,3,4,5
        int[] base_service_times = {4, 10, 15, 15};
        List<SimpleMatrix> lambdas = getLambdas(kLow, kHigh, r_values);
        lambdasToCSV(kLow, kHigh, r_values, lambdas, "rezultati_analiticki");
        for (SimpleMatrix lambda : lambdas)
            lambda.print("%.2f");

    }

}
