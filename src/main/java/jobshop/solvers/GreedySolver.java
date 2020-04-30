package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.JobNumbers;
import jobshop.encodings.ResourceOrder;

import java.util.ArrayList;

public class GreedySolver implements Solver {

    private Priority prio;




    public enum Priority {
        SPT, LPT, SRPT, LRPT,EST_SPT, EST_LRPT
    }

    public GreedySolver( Priority a){
        this.prio = a;

    }

    @Override
    public Result solve(Instance instance, long deadline) {
        int[] tempsAuPlusTot = new int[instance.numJobs];
        int[] tempsMachine = new int[instance.numMachines];
        JobNumbers sol = new JobNumbers(instance);
        int[] realisable  = new int[instance.numJobs+1];
        for(int a = 0; a<instance.numTasks*instance.numJobs;a++){
            int nj = this.nextJob(realisable,instance,tempsAuPlusTot,tempsMachine);
            sol.jobs[sol.nextToSet++] = nj;
            realisable[nj]++;
        }
        return new Result(instance, sol.toSchedule(), Result.ExitCause.Blocked);
    }

    private int nextJob(int[] realisable, Instance instance,int[] tempsAuPlusTot,int[] tempsMachine){
        int resJob = -1;
        boolean init = true;
        if( prio == Priority.SPT){
            int resTemps = -1;
            for(int a=0; a < instance.numJobs; a++){
                if(realisable[a] < instance.numTasks){
                    if(init){
                        init = false;
                        resJob = a;
                        resTemps = instance.duration(a,realisable[a]);
                    }else if(resTemps > instance.duration(a,realisable[a])){
                        resJob = a;
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
                        resJob = a;
                        resTemps = instance.duration(a,realisable[a]);
                    }else if(resTemps < instance.duration(a,realisable[a])){
                        resJob = a;
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
                        resJob = a;
                        for (int b = realisable[a]; b< instance.numTasks; b++){
                            resTempsTache += instance.duration(a,b);
                        }
                    }else{
                        int tempResTempsTache = 0;
                        for (int b = realisable[a]; b< instance.numTasks; b++){
                            tempResTempsTache += instance.duration(a,b);
                        }
                        if(resTempsTache>  tempResTempsTache){
                            resTempsTache = tempResTempsTache;
                            resJob = a;
                        }
                    }
                }
            }
        }else if (prio == Priority.LRPT){
            int resTempsTache = 0;
            for(int a= 0; a < instance.numJobs; a++){
                if(realisable[a] < instance.numTasks){
                    if(init){
                        init = false;
                        resJob = a;
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
                            resJob = a;
                        }
                    }
                }
            }
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
            resJob = resPossible.get(0);

            for(int z=0; z < resPossible.size(); z++){
                int a = resPossible.get(z);
                if(resTemps > instance.duration(a,realisable[a])){
                    resJob = a;
                    resTemps = instance.duration(a,realisable[a]);
                }
            }
            if(tempsAuPlusTot[resJob] + instance.duration(resJob,realisable[resJob]) > tempsMachine[instance.machine(resJob,realisable[resJob])] + instance.duration(resJob,realisable[resJob])){
                tempsAuPlusTot[resJob] = tempsAuPlusTot[resJob] + instance.duration(resJob,realisable[resJob]);
                tempsMachine[instance.machine(resJob,realisable[resJob])] = tempsAuPlusTot[resJob] + instance.duration(resJob,realisable[resJob]);
            }else{
                tempsAuPlusTot[resJob] = tempsMachine[instance.machine(resJob,realisable[resJob])] + instance.duration(resJob,realisable[resJob]);
                tempsMachine[instance.machine(resJob,realisable[resJob])] = tempsMachine[instance.machine(resJob,realisable[resJob])] + instance.duration(resJob,realisable[resJob]);
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
                        resJob = a;
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
                            resJob = a;
                        }
                    }
                }
            }

            if(tempsAuPlusTot[resJob] + instance.duration(resJob,realisable[resJob]) > tempsMachine[instance.machine(resJob,realisable[resJob])] + instance.duration(resJob,realisable[resJob])){
                tempsAuPlusTot[resJob] = tempsAuPlusTot[resJob] + instance.duration(resJob,realisable[resJob]);
                tempsMachine[instance.machine(resJob,realisable[resJob])] = tempsAuPlusTot[resJob] + instance.duration(resJob,realisable[resJob]);
            }else{
                tempsAuPlusTot[resJob] = tempsMachine[instance.machine(resJob,realisable[resJob])] + instance.duration(resJob,realisable[resJob]);
                tempsMachine[instance.machine(resJob,realisable[resJob])] = tempsMachine[instance.machine(resJob,realisable[resJob])] + instance.duration(resJob,realisable[resJob]);
            }
            //System.out.println("resJob: " + resJob + " temps du job: " + tempsAuPlusTot[resJob] + "\n");

        }
        return resJob;
    }


}
