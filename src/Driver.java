import lpsolve.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Driver {
    public static void main(String[] args) throws LpSolveException {
        /*int n = 8;
        int m = 4;
        double[] h = {100, 150, 175, 125, 180, 140, 120, 160};  // Demand
        double[] f = {4500, 4400, 4250, 4150};  // Fixed cost
        double[] k = {600, 700, 500, 550};  // Capacity
        double[][] c = {{26, 27, 28, 17}, {13, 19, 21, 19}, {19, 22, 12, 26}, {11, 22, 22, 24},
                {17, 15, 13, 15}, {25, 26, 27, 23}, {20, 19, 23, 26}, {21, 16, 23, 26}};  // Unit shipping cost (demand -> supply; actually alpha*d)
        */

        int n = 4;
        int m = 4;
        double[] h = {100, 120, 110, 130};
        double[] f = {1070, 1050, 900, 1200};
        double[] k = {10000, 10000, 10000, 10000};
        double[][] b = {{0, 14, 10, 999}, {14, 0, 999, 9}, {10, 999, 0, 12}, {999, 9, 12, 0}};
        double alpha = 0.7;

        Solver solver = new Solver(n, m, h, f, k, b, alpha);

        System.out.println(solver.solve());

        /*MasterProblem tpt = new MasterProblem(n, m, h, f, k, c);
        tpt.solve();
        System.out.println(tpt.getObj());
        double[] var = tpt.getVariables();
        System.out.println(Arrays.toString(var));*/

        /*TransportationProblem tpt = new TransportationProblem(n, m, h, f, k, c, x);
        tpt.solve();
        System.out.println(tpt.getObj());
        double[] var = tpt.getVariables();
        System.out.println(Arrays.toString(var));*/
    }
}
