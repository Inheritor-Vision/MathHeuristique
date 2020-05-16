package jobshop;

import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


import jobshop.solvers.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


public class Main {

    /** All solvers available in this program */
    private static HashMap<String, Solver> solvers;
    static {
        solvers = new HashMap<>();
        solvers.put("basic", new BasicSolver());
        solvers.put("random", new RandomSolver());
        solvers.put("spt", new GreedySolver(GreedySolver.Priority.SPT));
        solvers.put("lpt", new GreedySolver(GreedySolver.Priority.LPT));
        solvers.put("srpt", new GreedySolver(GreedySolver.Priority.SRPT));
        solvers.put("lrpt", new GreedySolver(GreedySolver.Priority.LRPT));
        solvers.put("est_spt", new GreedySolver(GreedySolver.Priority.EST_SPT));
        solvers.put("est_lrpt", new GreedySolver(GreedySolver.Priority.EST_LRPT));
        solvers.put("spt2", new GreedySolver2(GreedySolver2.Priority.SPT));
        solvers.put("lpt2", new GreedySolver2(GreedySolver2.Priority.LPT));
        solvers.put("srpt2", new GreedySolver2(GreedySolver2.Priority.SRPT));
        solvers.put("lrpt2", new GreedySolver2(GreedySolver2.Priority.LRPT));
        solvers.put("est_spt2", new GreedySolver2(GreedySolver2.Priority.EST_SPT));
        solvers.put("est_lrpt2", new GreedySolver2(GreedySolver2.Priority.EST_LRPT));
        solvers.put("descent", new DescentSolver());
        solvers.put("taboo1", new TabooSolver(10,100));
        solvers.put("taboo2", new TabooSolver(10,1000));
        solvers.put("taboo3", new TabooSolver(10,10000));
        solvers.put("taboo4", new TabooSolver(10,100000));
        solvers.put("taboo5", new TabooSolver(0,1000));
        solvers.put("taboo6", new TabooSolver(5,1000));
        solvers.put("taboo7", new TabooSolver(15,1000));
        solvers.put("taboo8", new TabooSolver(20,1000));
        solvers.put("taboo9", new TabooSolver(10,10));
        solvers.put("recuit1", new Recuit(100,100.0,0.99, 10));
        solvers.put("recuit2", new Recuit(1000,100.0,0.99, 10));
        solvers.put("recuit3", new Recuit(10000,100.0,0.99, 10));
        solvers.put("recuit4", new Recuit(50000,100.0,0.99, 10));
        solvers.put("recuit5", new Recuit(10000,50.0,0.8, 10));
        solvers.put("recuit6", new Recuit(10000,500.0,0.5, 10));
        solvers.put("recuit7", new Recuit(10000,100.0,0.7, 10));
        solvers.put("recuit8", new Recuit(10000,100.0,0.99, 1));
        solvers.put("recuit9", new Recuit(10000,50.0,0.8, 50));

        // add new solvers here
    }


    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("jsp-solver").build()
                .defaultHelp(true)
                .description("Solves jobshop problems.");

        parser.addArgument("-t", "--timeout")
                .setDefault(1L)
                .type(Long.class)
                .help("Solver timeout in seconds for each instance");
        parser.addArgument("--solver")
                .nargs("+")
                .required(true)
                .help("Solver(s) to use (space separated if more than one)");

        parser.addArgument("--instance")
                .nargs("+")
                .required(true)
                .help("Instance(s) to solve (space separated if more than one)");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        PrintStream output = System.out;

        long solveTimeMs = ns.getLong("timeout") * 1000;

        List<String> solversToTest = ns.getList("solver");
        for(String solverName : solversToTest) {
            if(!solvers.containsKey(solverName)) {
                System.err.println("ERROR: Solver \"" + solverName + "\" is not avalaible.");
                System.err.println("       Available solvers: " + solvers.keySet().toString());
                System.err.println("       You can provide your own solvers by adding them to the `Main.solvers` HashMap.");
                System.exit(1);
            }
        }
        List<String> instancePrefixes = ns.getList("instance");
        List<String> instances = new ArrayList<>();
        for(String instancePrefix : instancePrefixes) {
            List<String> matches = BestKnownResult.instancesMatching(instancePrefix);
            if(matches.isEmpty()) {
                System.err.println("ERROR: instance prefix \"" + instancePrefix + "\" does not match any instance.");
                System.err.println("       available instances: " + Arrays.toString(BestKnownResult.instances));
                System.exit(1);
            }
            instances.addAll(matches);
        }

        float[] runtimes = new float[solversToTest.size()];
        float[] distances = new float[solversToTest.size()];

        try {
            output.print(  "                         ");
            for(String s : solversToTest)
                output.printf("%-30s", s);
            output.println();
            output.print("instance size  best      ");
            for(String s : solversToTest) {
                output.print("runtime makespan ecart        ");
            }
            output.println();


            for(String instanceName : instances) {
                int bestKnown = BestKnownResult.of(instanceName);


                Path path = Paths.get("instances/", instanceName);
                Instance instance = Instance.fromFile(path);

                output.printf("%-8s %-5s %4d      ",instanceName, instance.numJobs +"x"+instance.numTasks, bestKnown);

                for(int solverId = 0 ; solverId < solversToTest.size() ; solverId++) {
                    String solverName = solversToTest.get(solverId);
                    Solver solver = solvers.get(solverName);
                    long start = System.currentTimeMillis();
                    long deadline = System.currentTimeMillis() + solveTimeMs;
                    Result result = solver.solve(instance, deadline);
                    long runtime = System.currentTimeMillis() - start;

                    if(!result.schedule.isValid()) {
                        System.err.println("ERROR: solver returned an invalid schedule");
                        System.exit(1);
                    }

                    assert result.schedule.isValid();
                    int makespan = result.schedule.makespan();
                    float dist = 100f * (makespan - bestKnown) / (float) bestKnown;
                    runtimes[solverId] += (float) runtime / (float) instances.size();
                    distances[solverId] += dist / (float) instances.size();

                    output.printf("%7d %8s %5.1f        ", runtime, makespan, dist);
                    output.flush();
                }
                output.println();

            }


            output.printf("%-8s %-5s %4s      ", "AVG", "-", "-");
            for(int solverId = 0 ; solverId < solversToTest.size() ; solverId++) {
                output.printf("%7.1f %8s %5.1f        ", runtimes[solverId], "-", distances[solverId]);
            }



        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
