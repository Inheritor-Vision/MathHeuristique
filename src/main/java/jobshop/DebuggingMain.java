package jobshop;

import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;
import jobshop.solvers.DescentSolver;
import jobshop.solvers.GreedySolver;
import jobshop.solvers.TabooSolver;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {

    public static void main(String[] args) {
        try {
            // load the aaa1 instance
            Instance instance = Instance.fromFile(Paths.get("instances/ft20"));

            //instances/aaa1:
            //2 3 # num-jobs num-tasks
            //0 3 1 3 2 2
            //1 2 0 2 2 4

            // construit une solution dans la représentation par
            // numéro de jobs : [0 1 1 0 0 1]
            // Note : cette solution a aussi été vue dans les exercices (section 3.3)
            //        mais on commençait à compter à 1 ce qui donnait [1 2 2 1 1 2]
            JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;

            //2.3
            //Au plus tot
            //0.0:0 0.1:3 0.2:6
            //1.0:0 1.1:3 1.2:8
            //Au plus tard
            //0:0.0:0 0.1:3 0.2:6
            //1:1.0:1 1.1:6 1.2:8

            /*System.out.println("\nENCODING: " + enc);

            Schedule sched = enc.toSchedule();*/
            // TODO: make it print something meaningful
            // by implementing the toString() method
            /*System.out.println("SCHEDULE: " + sched);
            System.out.println("VALID: " + sched.isValid());
            System.out.println("MAKESPAN: " + sched.makespan());*/

            TabooSolver ds = new TabooSolver(10,1000);
            Result res = ds.solve(instance, 100);
            GreedySolver gr = new GreedySolver(GreedySolver.Priority.SPT);
            Result res2 = gr.solve(instance,100);
            System.out.println("gr: " + res2.schedule.makespan() + "\nds: " + res.schedule.makespan());

            //ds.blocksOfCriticalPath(new ResourceOrder(instance));

            /*GreedySolver gr = new GreedySolver(GreedySolver.Priority.SPT);

            Result res = gr.solve(instance,100);
            System.out.print("Resultat:\n" + res.schedule.toString());

             gr = new GreedySolver(GreedySolver.Priority.EST_SPT);

             res = gr.solve(instance,100);
            System.out.print("Resultat:\n" + res.schedule.toString());*/

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
