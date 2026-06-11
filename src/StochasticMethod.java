import org.ejml.simple.SimpleMatrix;

public class StochasticMethod {
    
    private static final double K_PROBABILITY = 0.4;
    private static final SimpleMatrix BaseP = new SimpleMatrix(new double[][]{
            {0.15, 0.2, 0.15, 0.1}, 
            {0.35, 0.25, 0, 0}, 
            {0.35, 0, 0.25, 0}, 
            {0.35, 0, 0, 0.25}});
    private static SimpleMatrix getP(int k){
        SimpleMatrix P = new SimpleMatrix(BaseP.getNumRows() + k, BaseP.getNumCols() + k);
        P.insertIntoThis(0,0, BaseP);
        SimpleMatrix newColumns = new SimpleMatrix(BaseP.getNumRows(), k);
        newColumns.fill(K_PROBABILITY / k);
        P.insertIntoThis(0,BaseP.getNumCols(), newColumns);
        return P;
    }
    private static SimpleMatrix getA(int k){
        SimpleMatrix A = new SimpleMatrix(BaseP.getNumRows() + k, 1);
        A.set(0, 0, 1);
        return A;
    }
    public static void main(String[] args) {
        for(int k = 2; k < 6; k++){
            SimpleMatrix A = getA(k);
            SimpleMatrix P = getP(k);
            // lambda = (I - P^T)-1 * A
            SimpleMatrix lambda = SimpleMatrix.identity(P.getNumRows()).minus(P.transpose()).invert()
                    .mult(A);
            lambda.print("%.2f");
            
        }
    }
    
}
