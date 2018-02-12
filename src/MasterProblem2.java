import lpsolve.LpSolve;
import lpsolve.LpSolveException;

import java.util.ArrayList;

public class MasterProblem2 {
    int n;  // Number of demand nodes
    int m;  // Number of candidate sites
    double[] h;  // Demand[n]
    double[] f;  // Fixed cost[m]
    double[] k;  // Capacity[m]
    double[][] c;  // Unit shipping cost (demand -> supply; actually alpha*d)  [n][m]

    LpSolve solver;
    int solverCurrentConstraintSize;  // Constraint capacity when solver was initialized (need to expand if necessary)

    ArrayList<double[]> DX;
    ArrayList<Double> DConst;

    double obj;  // Result of objective function
    double[] var;  // Result of variables [m+1]   (0-indexed)

    public MasterProblem2(int n, int m, double[] h, double[] f, double[] k, double[][] c) throws LpSolveException {
        this.n = n;
        this.m = m;
        this.h = h;
        this.f = f;
        this.k = k;
        this.c = c;

        DX = new ArrayList<>();
        DConst = new ArrayList<>();

        //solverCurrentConstraintSize = 500;
        //solver = LpSolve.makeLp(solverCurrentConstraintSize, m);
        solver = LpSolve.makeLp(0, m + 1);
        solver.setVerbose(0);  // Disable output

        initializeSolver();
    }

    protected void initializeSolver() throws LpSolveException {
        double[] obj_func = new double[m + 2];  // The last variable is D
        for (int j = 0; j < m; j++) {
            obj_func[j + 1] = f[j];
            solver.setInt(j + 1, true);
            solver.setUpbo(j + 1, 1);
        }
        obj_func[m + 1] = 1;  // D
        solver.setObjFn(obj_func);
        solver.setMinim();
        solver.setDebug(true);  // Branch and bound

        double[] row = new double[m + 2];
        for (int j = 0; j < m; j++) {
            row[j + 1] = k[j];
        }
        double sumh = 0;
        for (int i = 0; i < n; i++)
            sumh += h[i];
        solver.addConstraint(row, LpSolve.GE, sumh);  // Sum of all X_js geq sum of h_i
    }

    public void solve() throws LpSolveException {
        solver.solve();
        solver.printObjective();
        solver.printSolution(1);
        solver.printConstraints(1);
        solver.printLp();

        obj = solver.getObjective();
        var = solver.getPtrVariables();
    }

    public double getObj() {
        return obj;
    }

    public double[] getVariables() {
        return var;
    }
}
