import lpsolve.*;

import java.io.*;
import java.util.*;

public class Driver {
    public static class demandNode implements Comparable<demandNode> {
        double x;
        double y;
        double size;
        public demandNode(double x, double y, double size) {
            this.x = x; this.y = y; this.size = size;
        }

        @Override
        public int compareTo(demandNode o) {
            if (Double.compare(x, o.x) != 0)
                return Double.compare(x, o.x);
            if (Double.compare(y, o.y) != 0)
                return Double.compare(y, o.y);
            return -Double.compare(size, o.size);
        }
    }
    public static class supplyNode implements Comparable<supplyNode>{
        double x;
        double y;
        boolean isSuper;
        int multi;  // Multipliers
        public supplyNode(double x, double y, boolean isSuper, int multi) {
            this.x = x; this.y = y; this.isSuper = isSuper; this.multi = multi;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || !(o instanceof supplyNode))
                return false;
            supplyNode sp = (supplyNode) o;
            return (x == sp.x) && (y == sp.y) && (isSuper == sp.isSuper) && (multi == sp.multi);
        }

        @Override
        public int compareTo(supplyNode o) {
            if (Double.compare(x, o.x) != 0)
                return Double.compare(x, o.x);
            if (Double.compare(y, o.y) != 0)
                return Double.compare(y, o.y);
            if (isSuper ^ o.isSuper)
                return (isSuper? -1: 1);  // Superchargers always greater than destination chargers
            return -Integer.compare(multi, o.multi);
        }
    }
    public static class DemandComparator implements Comparator<demandNode> {
        double dx;
        public DemandComparator(double dx) {this.dx = dx;}
        @Override
        public int compare(demandNode o1, demandNode o2) {
            boolean samex = false;
            if (sameX(o1.x, o2.x, dx))
                samex = true;
            if (Double.compare(o1.x, o2.x) != 0 && !samex)
                return Double.compare(o1.x, o2.x);
            if (Double.compare(o1.y, o2.y) != 0)
                return Double.compare(o1.y, o2.y);
            return -Double.compare(o1.size, o2.size);
        }
    }
    public static class SupplyComparator implements Comparator<supplyNode> {
        double dx;

        public SupplyComparator(double dx) {
            this.dx = dx;
        }

        @Override
        public int compare(supplyNode o1, supplyNode o2) {
            boolean samex = false;
            if (sameX(o1.x, o2.x, dx))
                samex = true;
            if (Double.compare(o1.x, o2.x) != 0 && !samex)
                return Double.compare(o1.x, o2.x);
            if (Double.compare(o1.y, o2.y) != 0)
                return Double.compare(o1.y, o2.y);
            if (o1.isSuper ^ o2.isSuper)
                return (o1.isSuper ? -1 : 1);  // Superchargers always greater than destination chargers
            return -Integer.compare(o1.multi, o2.multi);
        }
    }
    public static boolean sameX(double x1, double x2, double dx) {
        return Math.floor((x1 - minx) / dx) == Math.floor((x2 - minx) / dx);
    }

    static final double DCSCost = 2800;
    static final double SuperCost = 270000;
    static final int StationLife = 12;

    static final double DCSCars = 2;
    static final double DCSDuration = 62.5/16;
    static final double SuperCars = 8;
    static final double SuperDuration = 62.5/120;
    static final double vehicleRange = 265;
    static final double yearlyMiles = 13476;
    static final double availableHoursPerDay = 12;

    static final double costPerMile = 0.037;
    static final double publicChargingProportion = 0.19;
    //static final double electricMarketShare = 0.003;
    static final double electricMarketShare = 1;

    static final double fDCS = DCSCost / StationLife;
    static final double fSuper = SuperCost / StationLife;
    static final double kDCS = 365.2422 * availableHoursPerDay * DCSCars / DCSDuration / (yearlyMiles / vehicleRange);
    static final double kSuper = 365.2422 * availableHoursPerDay * SuperCars / SuperDuration / (yearlyMiles / vehicleRange);
    static final double alpha = costPerMile * (yearlyMiles / vehicleRange);

    static ArrayList<demandNode> demand;

    static double minx;
    static double maxx;
    static double miny;
    static double maxy;

    static ArrayList<supplyNode> supply;

    static double[] facilitySelection;
    static double[][] demandAllocation;


    public static void initialize() {
        demand = new ArrayList<>();
        supply = new ArrayList<>();
    }

    public static void readDemand(String demandInput) {
        // Extract locations from csv file
        minx = Integer.MAX_VALUE;
        maxx = Integer.MIN_VALUE;
        miny = Integer.MAX_VALUE;
        maxy = Integer.MIN_VALUE;
        try {
            Scanner s = new Scanner(new File(demandInput));
            s.useDelimiter(",");
            s.nextLine();
            while (s.hasNext()) {
                if (!s.hasNextDouble()) {  // Skips faulty lines without leading doubles
                    s.nextLine();
                    continue;
                }
                double x = s.nextDouble();
                double y = s.nextDouble();
                String str = s.next();
                s.nextLine();
                int z;
                /*try {
                    z = Integer.parseInt(str.replaceAll("\"Vehicle Spaces: ", ""));
                } catch (NumberFormatException ee) {
                    z = 1;
                }*/
                try {
                    z = Integer.parseInt(str.substring(0, str.indexOf("/")));
                } catch (NumberFormatException ee) {
                    z = 1;
                }

                demand.add(new demandNode(x, y, z));
                minx = Math.min(minx, x); maxx = Math.max(maxx, x);
                miny = Math.min(miny, y); maxy = Math.max(maxy, y);
            }
            s.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void readSupply(String supplyInput, boolean bounded, boolean isSuper) {
        // Extract locations from csv file
        try {
            Scanner s = new Scanner(new File(supplyInput));
            s.useDelimiter(",");
            s.nextLine();
            while (s.hasNext()) {
                if (!s.hasNextDouble()) {  // Skips faulty lines without leading doubles
                    s.nextLine();
                    continue;
                }
                double x = s.nextDouble();
                double y = s.nextDouble();
                s.nextLine();

                if (bounded)
                    if (!(x>=minx && x<=maxx) || !(y>=miny && y<=maxy))
                        continue;

                supply.add(new supplyNode(x, y, isSuper, 1));
            }
            s.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void solve() throws LpSolveException {
        int n = demand.size();
        int m = supply.size();
        System.out.println(n + " " + m);

        // Create demand array
        double[] h = new double[n];
        for (int i=0; i<n; i++)
            h[i] = demand.get(i).size * electricMarketShare * publicChargingProportion;

        // Create supply array
        double[] f = new double[m];
        double[] k = new double[m];
        for (int i=0; i<m; i++) {
            if (supply.get(i).isSuper) {
                f[i] = fSuper * supply.get(i).multi;
                k[i] = kSuper * supply.get(i).multi;
            } else {
                f[i] = fDCS * supply.get(i).multi;
                k[i] = kDCS * supply.get(i).multi;
            }
        }

        // Create distance array
        double[][] d = new double[n][m];
        for (int i=0; i<n; i++)
            for (int j=0; j<m; j++)
                d[i][j] = DistanceCalculator.distance(demand.get(i).x, demand.get(i).y, supply.get(j).x, supply.get(j).y);

        // Solve
        Solver sol = new Solver(n, m, h, f, k, d, alpha);
        sol.solve();
        facilitySelection = sol.getFacilitySelection();
        demandAllocation = sol.getDemandAllocation();
    }

    public static void mergeDemand(double dx, double dy) {
        Collections.sort(demand);

        ArrayList<demandNode> newNodes = new ArrayList<>();
        double sumx = 0;
        double sumy = 0;
        double sumsize = 0;
        int sumn = 0;
        int prevIndex = -1;
        for (int i=0; i<demand.size(); i++) {
            if (i == 0 || !sameX(demand.get(prevIndex).x, demand.get(i).x, dx) || (demand.get(i).y - demand.get(prevIndex).y > dy)) {
                // Sum existing ones
                if (sumn != 0) {
                    newNodes.add(new demandNode(sumx / sumn, sumy / sumn, sumsize));
                    sumx = 0;
                    sumy = 0;
                    sumsize = 0;
                    sumn = 0;
                }
                prevIndex = i;
            }
            sumx += demand.get(i).x;
            sumy += demand.get(i).y;
            sumsize += demand.get(i).size;
            sumn++;
        }
        if (sumn != 0) {
            newNodes.add(new demandNode(sumx / sumn, sumy / sumn, sumsize));
        }
        demand = newNodes;
    }

    public static void mergeSupply(double dx, double dy) {
        Collections.sort(supply);

        ArrayList<supplyNode> newNodes = new ArrayList<>();
        double sumxDCS = 0;
        double sumyDCS = 0;
        int sumsizeDCS = 0;
        int sumnDCS = 0;
        double sumxSuper = 0;
        double sumySuper = 0;
        int sumsizeSuper = 0;
        int sumnSuper = 0;
        int prevIndex = -1;
        for (int i=0; i<supply.size(); i++) {
            if (i == 0 || !sameX(supply.get(prevIndex).x, supply.get(i).x, dx) || (supply.get(i).y - supply.get(prevIndex).y > dy)) {
                // Sum existing ones
                if (sumnDCS != 0) {
                    newNodes.add(new supplyNode(sumxDCS / sumnDCS, sumyDCS / sumnDCS, false, sumsizeDCS));
                    sumxDCS = 0;
                    sumyDCS = 0;
                    sumsizeDCS = 0;
                    sumnDCS = 0;
                }
                if (sumnSuper != 0) {
                    newNodes.add(new supplyNode(sumxSuper / sumnSuper, sumySuper / sumnSuper, true, sumsizeSuper));
                    sumxSuper = 0;
                    sumySuper = 0;
                    sumsizeSuper = 0;
                    sumnSuper = 0;
                }
                prevIndex = i;
            }
            if (supply.get(i).isSuper) {
                sumxSuper += supply.get(i).x;
                sumySuper += supply.get(i).y;
                sumsizeSuper += supply.get(i).multi;
                sumnSuper++;
            } else {
                sumxDCS += supply.get(i).x;
                sumyDCS += supply.get(i).y;
                sumsizeDCS += supply.get(i).multi;
                sumnDCS++;
            }
        }
        if (sumnDCS != 0) {
            newNodes.add(new supplyNode(sumxDCS / sumnDCS, sumyDCS / sumnDCS, false, sumsizeDCS));
        }
        if (sumnSuper != 0) {
            newNodes.add(new supplyNode(sumxSuper / sumnSuper, sumySuper / sumnSuper, true, sumsizeSuper));
        }
        supply = newNodes;
    }

    public static void outputSuperchargers(String output) {
        int sum = 0;

        try {
            FileWriter file = new FileWriter(output);
            PrintWriter pt = new PrintWriter(file);
            pt.println("latitude,longitude");
            for (int i=0; i<supply.size(); i++) {
                if (facilitySelection[i] < 0.5) continue;
                supplyNode s = supply.get(i);
                if (s.isSuper) {
                    sum += s.multi;
                    for (int j = 0; j < s.multi; j++)
                        pt.println(s.x + "," + s.y);
                }
            }
            pt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Number of superchargers: " + sum);
    }

    public static void outputDCS(String output) {
        int sum = 0;

        try {
            FileWriter file = new FileWriter(output);
            PrintWriter pt = new PrintWriter(file);
            pt.println("latitude,longitude");
            for (int i=0; i<supply.size(); i++) {
                if (facilitySelection[i] < 0.5) continue;
                supplyNode s = supply.get(i);
                if (!s.isSuper) {
                    sum += s.multi;
                    for (int j = 0; j < s.multi; j++)
                        pt.println(s.x + "," + s.y);
                }
            }
            pt.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Number of DCS: " + sum);
    }

    public static void generateRandomDCS(int sum) {
        double dx = 0.01;
        Random random = new Random();
        for (int i=0; i<sum; i++) {
            int ind = random.nextInt(demand.size());
            supply.add(new supplyNode(demand.get(ind).x + random.nextDouble()*dx, demand.get(ind).y + random.nextDouble()*dx, false, 1));
        }
    }
    public static void generateRandomSuper(int sum) {
        double dx = 0.01;
        Random random = new Random();
        for (int i=0; i<sum; i++) {
            int ind = random.nextInt(demand.size());
            supply.add(new supplyNode(demand.get(ind).x + random.nextDouble()*dx, demand.get(ind).y + random.nextDouble()*dx, true, 1));
        }
    }

    public static void main(String[] args) throws LpSolveException {
        initialize();

        readDemand("data/wilson_population.csv");

        readSupply("data/supercharger.csv", true, true);
        readSupply("data/destination.csv", true, false);

        generateRandomDCS(53);
        generateRandomSuper(7);

        // If data size is too large, need to merge some demand nodes and/or supply nodes

        //if (demand.size() > 150)
        //    mergeDemand(0.0001, 0.0001);
        //if (supply.size() > 50)
            //mergeSupply(0.025, 0.025);
            //mergeSupply(0.0053, 0.0053);
        //    mergeSupply(0.008, 0.008);

        solve();

        outputSuperchargers("data/Wilson_100_superchargers_new.csv");
        outputDCS("data/Wilson_100_DCS_new.csv");
    }
}
