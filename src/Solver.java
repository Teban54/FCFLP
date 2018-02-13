import lpsolve.LpSolveException;
import net.sf.javailp.SolverFactoryLpSolve;

import java.util.ArrayList;
import java.util.Arrays;

public class Solver {
    int n;  // Number of demand nodes
    int m;  // Number of candidate sites
    double[] h;  // Demand[n]
    double[] f;  // Fixed cost[m]
    double[] k;  // Capacity[m]
    double[][] d;  //  Distance (demand -> supply)  [n][m]
    double alpha;  //  Unit cost

    double[][] c;  // Unit cost  [n][m]

    double[] x;  // Choice of stations[m]  (Must be integer)
    double[][] y;  // Proportion of demand served by charging station  [n][m]
    double sol;  // Solution

    MasterProblem master;
    TransportationProblem tpt;

    public Solver(int n, int m, double[] h, double[] f, double[] k, double[][] d, double alpha) throws LpSolveException {
        this.n = n;
        this.m = m;
        this.h = h;
        this.f = f;
        this.k = k;
        this.d = d;
        this.alpha = alpha;

        c = new double[n][m];
        for (int i=0; i<n; i++)
            for (int j=0; j<m; j++)
                c[i][j] = d[i][j] * alpha;

        x = new double[m];
        y = new double[n][m];
    }

    public double solve() throws LpSolveException {
        double low = -Integer.MAX_VALUE;
        double high = Integer.MAX_VALUE;

        master = new MasterProblem(n, m, h, f, k, c);

        System.out.println(low + " " + high);
        int t = 0;  // If lower and upper bounds remain unchanged after 30 iterations, stop
        int t2 = 0;  // If upper bound remains unchanged after 100 iterations, stop
        double prevlow = low;
        double prevhigh = high;
        while (low < high - 1e-6 && t < 30 && t2 < 100) {
            master.solve();
            low = master.getObj();
            x = Arrays.copyOf(master.getVariables(), m);

            tpt = new TransportationProblem(n, m, h, f, k, c, x);
            tpt.solve();

            // Convert [n*m] to [n][m]
            double[] var = tpt.getVariables();
            for (int i=0; i<n; i++)
                for (int j=0; j<m; j++)
                    y[i][j] = var[i*m+j];

            high = Math.min(high, tpt.getTotalCost());

            System.out.println(low + " " + high);
            if (low < high - 1e-6) {
                double cst = 0;
                double[] u = tpt.getUs();
                double[] w = tpt.getWs();
                for (int i=0; i<n; i++)
                    cst += h[i] * u[i];
                double[] coeff = new double[m];
                for (int j=0; j<m; j++)
                    coeff[j] = k[j]*w[j];
                master.addD(cst, coeff);
            }
            if (low == prevlow && high == prevhigh) t++; else t=0;
            if (high == prevhigh) t2++; else t2=0;
        }

        // The exact solutions are stored in x and y
        sol = high;
        return sol;
    }

    public double[] getFacilitySelection() {
        return x;
    }

    public double[][] getDemandAllocation() {
        return y;
    }
}
