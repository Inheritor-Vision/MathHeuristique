package jobshop.solvers;

import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Recuit implements Solver {

    final double T;
    final int maxIter;
    final double redFact;
    final int redIter;

    public Recuit(int mI, double T,double redFact, int redIter){
            this.T = T;
            this.redFact = redFact;
            this.redIter = redIter;
            maxIter = mI;
    }

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
        @Override
        public String toString() {
            return("("+ machine + "," + firstTask + "," + lastTask + ")");
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        public void applyOn(ResourceOrder order) {

            Task temp = order.tasksByMachine[machine][t1];
            order.tasksByMachine[machine][t1] = order.tasksByMachine[machine][t2];
            order.tasksByMachine[machine][t2] = temp;


        }
    }


    @Override
    public Result solve( Instance instance, long deadline) {
        GreedySolver2 gr = new GreedySolver2(GreedySolver2.Priority.EST_LRPT);
        Result res = gr.solve(instance,deadline);



        int k =  0;
        double T = this.T;
        ResourceOrder savedRo = ResourceOrder.fromSchedule(res.schedule);
        int saved_makespan = savedRo.toSchedule().makespan();
        ResourceOrder bestRo = savedRo.copy();
        int best_makespan = saved_makespan;
        boolean changed = true;
        List<Block> lb = null;

        while ( k < maxIter){
            if(changed){
                lb = blocksOfCriticalPath(savedRo);
                if(saved_makespan<best_makespan){
                    best_makespan = saved_makespan;
                    bestRo = savedRo;
                }
            }
            int randomNum = ThreadLocalRandom.current().nextInt(lb.size());
            List<Swap> ls = neighbors(lb.get(randomNum));
            randomNum = ThreadLocalRandom.current().nextInt(ls.size());
            ResourceOrder tempRo = savedRo.copy();
            ls.get(randomNum).applyOn(tempRo);
            changed = false;
            int tempRomakespan = tempRo.toSchedule().makespan();
            if(tempRomakespan <= saved_makespan){
                savedRo = tempRo;
                saved_makespan = tempRomakespan;
                changed = true;
            }else {

                if(ThreadLocalRandom.current().nextDouble() <= Math.exp(((double)saved_makespan - tempRomakespan)/T)){
                    savedRo = tempRo;
                    saved_makespan = tempRomakespan;
                    changed = true;
                }
                if(k % redIter == 0){
                    T = redFact * T;
                }
            }
            k++;


        }

        return new Result(instance, bestRo.toSchedule(), Result.ExitCause.Blocked);


    }

    /** Returns a list of all blocks of the critical path. */
    public List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> tl  = order.toSchedule().criticalPath();
        LinkedList<Block> res = new LinkedList<Block>();
        int lastMachine = order.instance.machine(tl.get(0).job,tl.get(0).task);
        Task firstTask  = tl.get(0);
        int index = 0;
        for (int a = 1; a < tl.size(); a++){
            if(order.instance.machine(tl.get(a).job, tl.get(a).task) == lastMachine){
                index++;
            }else if(index > 0){
                int ind = 0;
                boolean found = false;
                while(ind < order.instance.numJobs && !found){
                    if(order.tasksByMachine[lastMachine][ind].task == firstTask.task && order.tasksByMachine[lastMachine][ind].job == firstTask.job){
                        res.addLast(new Block(lastMachine,ind,ind+index));
                        found = true;
                    }
                    ind ++;
                }
                index = 0;
                firstTask = tl.get(a);
                lastMachine = order.instance.machine(tl.get(a).job,tl.get(a).task);
            }else{
                index = 0;
                firstTask = tl.get(a);
                lastMachine = order.instance.machine(tl.get(a).job,tl.get(a).task);
            }
        }
        if(index > 0){
            int ind = 0;
            boolean found = false;
            while(ind < order.instance.numJobs && !found){
                if(order.tasksByMachine[lastMachine][ind].task == firstTask.task && order.tasksByMachine[lastMachine][ind].job == firstTask.job){
                    res.addLast(new Block(lastMachine,ind,ind+index));
                    found = true;
                }
                ind ++;
            }
        }

        return res;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    List<Swap> neighbors(Block block) {
        LinkedList<Swap> res = new LinkedList<Swap>();
        if(block.lastTask - block.firstTask  == 1){
            res.addLast(new Swap(block.machine, block.firstTask, block.lastTask));
        }else{
            res.addLast(new Swap(block.machine, block.firstTask, block.firstTask+1));
            res.addLast(new Swap(block.machine, block.lastTask-1, block.lastTask));
        }
        return res;
    }

}
