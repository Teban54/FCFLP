import lpsolve.*;
import net.sf.javailp.*;
import net.sf.javailp.Solver;

import java.util.ArrayList;

public class MasterProblem {
    int n;  // Number of demand nodes
    int m;  // Number of candidate sites
    double[] h;  // Demand[n]
    double[] f;  // Fixed cost[m]
    double[] k;  // Capacity[m]
    double[][] c;  // Unit shipping cost (demand -> supply; actually alpha*d)  [n][m]

    //LpSolve solver;
    //int solverCurrentConstraintSize;  // Constraint capacity when solver was initialized (need to expand if necessary)

    SolverFactory factory;
    Problem problem;
    Result result;

    ArrayList<double[]> DX;
    ArrayList<Double> DConst;

    double obj;  // Result of objective function
    double[] var;  // Result of variables [m+1]   (0-indexed)

    public MasterProblem(int n, int m, double[] h, double[] f, double[] k, double[][] c) throws LpSolveException {
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
        factory = new SolverFactoryLpSolve();
        factory.setParameter(Solver.VERBOSE, 0);
        factory.setParameter(Solver.TIMEOUT, 100);

        initializeSolver();
    }

    protected void initializeSolver() throws LpSolveException {
        problem = new Problem();

        Linear linear = new Linear();
        for (int j = 0; j < m; j++) {
            linear.add(f[j], Integer.toString(j+1));
        }
        linear.add(1, Integer.toString(m+1));
        problem.setObjective(linear, OptType.MIN);

        linear = new Linear();
        for (int j = 0; j < m; j++) {
            linear.add(k[j], Integer.toString(j+1));
        }
        double sumh = 0;
        for (int i = 0; i < n; i++)
            sumh += h[i];
        problem.add(linear, ">=", sumh);

        for (int j=1; j<=m+1; j++) {
            problem.setVarType(Integer.toString(j), Integer.class);
            problem.setVarLowerBound(Integer.toString(j), 0);
            if (j != m+1) problem.setVarUpperBound(Integer.toString(j), 1);
        }
    }

    public void solve() throws LpSolveException {
        Solver solver = factory.get();
        Result result = solver.solve(problem);
        obj = result.getObjective().doubleValue();
        var = new double[m+1];
        for (int j=1; j<=m+1; j++)
            var[j-1] = result.getPrimalValue(Integer.toString(j)).doubleValue();
    }

    public void addD(double cst, double[] coeff) {  // coeff should be positive!!!
        Linear linear = new Linear();
        for (int j = 0; j < m; j++) {
            linear.add(coeff[j], Integer.toString(j+1));
        }
        linear.add(1, Integer.toString(m+1));
        problem.add(linear, ">=", cst);
    }

    public double getObj() {
        return obj;
    }

    public double[] getVariables() {
        return var;
    }
}
