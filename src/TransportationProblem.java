import lpsolve.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class TransportationProblem {
    int n;  // Number of demand nodes
    int m;  // Number of candidate sites
    double[] h;  // Demand[n]
    double[] f;  // Fixed cost[m]
    double[] k;  // Capacity[m]
    double[][] c;  // Unit shipping cost (demand -> supply; actually alpha*d)  [n][m]

    double[] x;  // Choice of stations[m]  (Must be integer)

    LpSolve solver;

    double obj;  // Result of objective function
    double[] var;  // Result of variables [n*m]   (0-indexed)
    double[] u;  // Result of demand-side duals
    double[] w;  // Result of supply-side duals

    public TransportationProblem(int n, int m, double[] h, double[] f, double[] k, double[][] c, double[] x) throws LpSolveException {
        this.n = n;
        this.m = m;
        this.h = h;
        this.f = f;
        this.k = k;
        this.c = c;
        this.x = x;

        solver = LpSolve.makeLp(n+m, n*m);
        solver.setVerbose(0);  // Disable output

        initializeSolver();
    }

    protected void initializeSolver() throws LpSolveException {
        double[] obj_func = new double[n*m+1];
        for (int i=0; i<n; i++)
            for (int j=0; j<m; j++)
                obj_func[i*m+j+1] = c[i][j];
        solver.setObjFn(obj_func);
        solver.setMinim();

        for (int i=0; i<n; i++) {
            double[] row = new double[n*m+1];
            for (int j=0; j<m; j++)
                row[i*m+j+1] = 1;
            solver.addConstraint(row, LpSolve.EQ, h[i]);  // Sum over j equals h_i
        }
        for (int j=0; j<m; j++) {
            double[] row = new double[n*m+1];
            for (int i=0; i<n; i++)
                row[i*m+j+1] = 1;
            solver.addConstraint(row, LpSolve.LE, k[j]*x[j]);  // Sum over i leq k_j*X_j
        }
        //for (int i=0; i<n*m; i++)
        //    solver.setLowbo(i, 0);
        // Exception in thread "main" lpsolve.LpSolveException: ERROR in set_lowbo: status = -1 (Model has not been optimized)
    }

    public void solve() throws LpSolveException {
        solver.solve();

        obj = solver.getObjective();
        var = solver.getPtrVariables();

        double[] duals = new double[(n+m)*2+n*m+1];
        solver.getDualSolution(duals);
        u = Arrays.copyOfRange(duals, n+m+1, n*2+m+1);
        w = Arrays.copyOfRange(duals, n*2+m+1, n*2+m*2+1);
        for (int i=0; i<m; i++)
            w[i] = -w[i];
    }

    public double getObj() {
        return obj;
    }

    public double[] getVariables() {
        return var;
    }

    public double getTotalCost() {
        double sum = obj;
        for (int i=0; i<m; i++) {
            sum += f[i] * x[i];
        }
        return sum;
    }

    public HashMap<Integer, Double> getSupplyContributions(int dmd) {  // Returns proportions of demand served by each supply node
        HashMap<Integer, Double> map = new HashMap<>();
        for (int j=0; j<m; j++)
            if (var[dmd*m+j] > 1e-6) {
                map.put(j, var[dmd*m+j]/h[j]);
            }
        return map;
    }

    public double[] getUs() {
        return u;
    }
    public double[] getWs() {
        return w;
    }
}
