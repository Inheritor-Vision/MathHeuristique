package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;


public class GreedySolver2 implements Solver {

    private Priority prio;




    public enum Priority {
        SPT, LPT, SRPT, LRPT,EST_SPT, EST_LRPT
    }

    public GreedySolver2(Priority a){
        this.prio = a;

    }

    @Override
    public Result solve(Instance instance, long deadline) {
        int[] tempsAuPlusTot = new int[instance.numJobs];
        int[] tempsMachine = new int[instance.numMachines];
        ResourceOrder sol = new ResourceOrder(instance);
        int[] realisable  = new int[instance.numJobs+1];

        int[] tempsTache = new int[instance.numJobs];
        if(this.prio == Priority.LRPT || this.prio == Priority.SRPT){
            for( int z = 0; z < instance.numJobs; z++){
                for (int b = realisable[z]; b< instance.numTasks; b++){
                    tempsTache[z] += instance.duration(z,b);
                }
            }
        }

        int indexMachine[] = new int[instance.numMachines];

        for(int a = 0; a<instance.numTasks*instance.numJobs;a++){
            Task nj = this.nextJob(realisable,instance,tempsAuPlusTot,tempsMachine,tempsTache);
            int mach = instance.machine(nj);
            sol.tasksByMachine[mach][indexMachine[mach]] = nj;
            indexMachine[mach]++;
            realisable[nj.job]++;
        }
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    private Task nextJob(int[] realisable, Instance instance,int[] tempsAuPlusTot,int[] tempsMachine, int[] tempsTache){
        Task resJob = new Task(-1,-1);
        boolean init = true;
        if( prio == Priority.SPT){
            int resTemps = -1;
            for(int a=0; a < instance.numJobs; a++){
                if(realisable[a] < instance.numTasks){
                    if(init){
                        init = false;
                        resJob = new Task(a,realisable[a]);
                        resTemps = instance.duration(a,realisable[a]);
                    }else if(resTemps > instance.duration(a,realisable[a])){
                        resJob = new Task(a,realisable[a]);
                        resTemps = instance.duration(a,realisable[a]);
                    }
                }
            }

        }else if( prio == Priority.LPT){
            int resTemps = -1;
            for(int a=0; a < instance.numJobs; a++){
                if(realisable[a] < instance.numTasks){
                    if(init){
                        init = false;
                        resJob = new Task(a,realisable[a]);
                        resTemps = instance.duration(a,realisable[a]);
                    }else if(resTemps < instance.duration(a,realisable[a])){
                        resJob = new Task(a,realisable[a]);
                        resTemps = instance.duration(a,realisable[a]);
                    }
                }
            }
        }else if(prio == Priority.SRPT){
            int resTempsTache = 0;
            for(int a= 0; a < instance.numJobs; a++){
                if(realisable[a] <instance.numTasks){
                    if(init){
                        init = false;
                        resJob = new Task(a,realisable[a]);
                        resTempsTache =  tempsTache[a];

                    }else{

                        if(resTempsTache>  tempsTache[a]){
                            resTempsTache = tempsTache[a];
                            resJob = new Task(a,realisable[a]);
                        }
                    }
                }
            }
            tempsTache[resJob.job] -= instance.duration(resJob.job,resJob.task);

        }else if (prio == Priority.LRPT){
            int resTempsTache = 0;
            for(int a= 0; a < instance.numJobs; a++){
                if(realisable[a] <instance.numTasks){
                    if(init){
                        init = false;
                        resJob = new Task(a,realisable[a]);
                        resTempsTache =  tempsTache[a];

                    }else{

                        if(resTempsTache<  tempsTache[a]){
                            resTempsTache = tempsTache[a];
                            resJob = new Task(a,realisable[a]);
                        }
                    }
                }
            }
            tempsTache[resJob.job] -= instance.duration(resJob.job,resJob.task);

        }else if(prio == Priority.EST_SPT){
            int resTempsPlusTot = 0;
            ArrayList<Integer> resPossible = new ArrayList<Integer>(0);
            for(int a = 0; a < instance.numJobs; a++){
                if(realisable[a] < instance.numTasks) {
                    if(init){
                        init = false;
                        resPossible.add(a);
                        resTempsPlusTot = Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] );
                    }else if(resTempsPlusTot> Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] )){
                        resTempsPlusTot = Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] );
                        resPossible = new ArrayList<Integer>(0);
                        resPossible.add(a);
                    }
                    else if(resTempsPlusTot == Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] )){
                        resPossible.add(a);
                    }
                }

            }
            int resTemps = instance.duration(resPossible.get(0),realisable[resPossible.get(0)]);
            resJob = new Task(resPossible.get(0),realisable[resPossible.get(0)]);

            for(int z=0; z < resPossible.size(); z++){
                int a = resPossible.get(z);
                if(resTemps > instance.duration(a,realisable[a])){
                    resJob = new Task(a,realisable[a]);
                    resTemps = instance.duration(a,realisable[a]);
                }
            }
            if(tempsAuPlusTot[resJob.job] + instance.duration(resJob.job,resJob.task) > tempsMachine[instance.machine(resJob.job,resJob.task)] + instance.duration(resJob.job,resJob.task)){
                tempsAuPlusTot[resJob.job] = tempsAuPlusTot[resJob.job] + instance.duration(resJob.job,resJob.task);
                tempsMachine[instance.machine(resJob.job,resJob.task)] = tempsAuPlusTot[resJob.job] + instance.duration(resJob.job,resJob.task);
            }else{
                tempsAuPlusTot[resJob.job] = tempsMachine[instance.machine(resJob.job,resJob.task)] + instance.duration(resJob.job,resJob.task);
                tempsMachine[instance.machine(resJob.job,resJob.task)] = tempsMachine[instance.machine(resJob.job,resJob.task)] + instance.duration(resJob.job,resJob.task);
            }
        }else {
            int resTempsPlusTot = 0;
            ArrayList<Integer> resPossible = new ArrayList<Integer>(0);
            for(int a = 0; a < instance.numJobs; a++){
                if(realisable[a] < instance.numTasks) {
                    if(init){
                        init = false;
                        resPossible.add(a);
                        resTempsPlusTot = Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] );
                    }else if(resTempsPlusTot> Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] )){
                        resTempsPlusTot = Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] );
                        resPossible = new ArrayList<Integer>(0);
                        resPossible.add(a);
                    }
                    else if(resTempsPlusTot == Math.max(tempsAuPlusTot[a] , tempsMachine[instance.machine(a,realisable[a])] )){
                        resPossible.add(a);
                    }
                }

            }
            //System.out.println("restempsplustot: "+ resTempsPlusTot +" size:" + resPossible.size());

            init= true;
            int resTempsTache = 0;
            for(int z=0; z < resPossible.size(); z++){
                int a = resPossible.get(z);
                if(realisable[a] < instance.numTasks){
                    if(init){
                        init = false;
                        resJob = new Task (a,realisable[a]);
                        for (int b = realisable[a]; b< instance.numTasks; b++){
                            resTempsTache += instance.duration(a,b);
                        }
                    }else{
                        int tempResTempsTache = 0;
                        for (int b = realisable[a]; b< instance.numTasks; b++){
                            tempResTempsTache += instance.duration(a,b);
                        }
                        if(resTempsTache <  tempResTempsTache){
                            resTempsTache = tempResTempsTache;
                            resJob = new Task (a,realisable[a]);
                        }
                    }
                }
            }

            if(tempsAuPlusTot[resJob.job] + instance.duration(resJob.job,resJob.task) > tempsMachine[instance.machine(resJob.job,resJob.task)] + instance.duration(resJob.job,resJob.task)){
                tempsAuPlusTot[resJob.job] = tempsAuPlusTot[resJob.job] + instance.duration(resJob.job,resJob.task);
                tempsMachine[instance.machine(resJob.job,resJob.task)] = tempsAuPlusTot[resJob.job] + instance.duration(resJob.job,resJob.task);
            }else{
                tempsAuPlusTot[resJob.job] = tempsMachine[instance.machine(resJob.job,resJob.task)] + instance.duration(resJob.job,resJob.task);
                tempsMachine[instance.machine(resJob.job,resJob.task)] = tempsMachine[instance.machine(resJob.job,resJob.task)] + instance.duration(resJob.job,resJob.task);
            }
            //System.out.println("resJob: " + resJob + " temps du job: " + tempsAuPlusTot[resJob] + "\n");

        }
        return resJob;
    }


}
