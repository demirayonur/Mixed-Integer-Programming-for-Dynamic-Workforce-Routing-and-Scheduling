package mip;

import java.util.stream.IntStream;


public class PeriodMIP {
	
	/*
	 * This class includes sets and parameters of the problem.
	 * Whenever a problem is desired to be solved by the
	 * MIP model developed in 'MIP.java', the problem
	 * instance should be created through this class and then
	 * be introduced to 'MIP.java'.
	 */
	
	// Numbers
	int n_task;
	int n_team;
	int n_skill;
	
	int[] N;
	int[] N_prime;
	int[] K;
	int[] Q;
	
	// Parameters
	int[] starting_times; // |K|
	int[] arrival_times;  // |N|
	int[] process_times;  // |N|
	int[] priority;  // |N|
	int t_current;
	int t_end;
	
	int[][] dummy_travel; // |K| x |N_prime|
	int[][] travel_times; // |N_prime| x |N_prime|
	int[][] task_skill;  // |N_prime| x |Q|
	int[][] team_skill;  // |K| x |Q|
	
	public PeriodMIP(int n_task, int n_team, int n_skill, int[] starting_times, int[] arrival_times, int[] process_times, int[] priority,
			       int t_current, int t_end, int[][] travel_times, int[][] task_skill, int[][] team_skill, int[][] dummy_travel) {
		
		// Constructor Method
		this.n_task = n_task;
		this.n_team = n_team;
		this.n_skill = n_skill;
		
		N = IntStream.range(1, n_task+1).toArray();
		N_prime = IntStream.range(0, n_task+1).toArray();
		K = IntStream.range(0, n_team).toArray();
		Q = IntStream.range(0, n_skill).toArray();
		
		this.starting_times = starting_times;
		this.arrival_times = arrival_times;
		this.process_times = process_times;
		this.priority = priority;
		this.t_current = t_current;
		this.t_end = t_end;
		this.travel_times = travel_times;
		this.task_skill = task_skill;
		this.team_skill = team_skill;
		this.dummy_travel = dummy_travel;
	}
	
	
}


