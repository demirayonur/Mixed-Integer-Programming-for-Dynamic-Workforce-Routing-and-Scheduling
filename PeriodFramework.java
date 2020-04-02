package mip;

import java.util.*;

import gurobi.GRBException;

public class PeriodFramework {
	
	int n_period;
	ArrayList<Task> new_tasks;
	ArrayList<Task> whole_tasks;
	int t_now;
	
	public PeriodFramework(int n_period, ArrayList<Task> new_tasks) {
		this.n_period = n_period;
		this.new_tasks = new_tasks;
		whole_tasks = new ArrayList<Task>();
		if(n_period==0) {
			t_now=0;
		}else {
			t_now = new_tasks.get(new_tasks.size()-1).arrival_time;
		}
		for(Task t: new_tasks) {
			whole_tasks.add(t);
		}
	}
	
	public void add_residual_tasks(ArrayList<Task> residual) {
		
		if(residual.size() > 0) {
			for(Task t: residual) {
				whole_tasks.add(t);
			}
		}	
	}
	
	public PeriodMIP convert(Problem problem, int[] starting_times, Task[] starting_nodes, ArrayList<Task> residual, int t_end) {
		
		add_residual_tasks(residual);
		
		HashMap<Integer, Task> index_task_map = new HashMap<Integer, Task>();
		HashMap<Task, Integer> task_index_map = new HashMap<Task, Integer>();
		
		int temp_count = 1;
		for(Task t: whole_tasks) {
			index_task_map.put(temp_count, t);
			task_index_map.put(t, temp_count);
			temp_count ++;
		}
		int n_task = whole_tasks.size();
		int n_team = problem.teams.length;
		int n_skill = whole_tasks.get(0).skill_req.length;
		
		int[] arrival_times = new int[n_task+1];
		int[] process_times = new int[n_task+1];
		int[] priority = new int[n_task+1];
		int[][] task_skill = new int[n_task+1][n_skill];
		arrival_times[0]=0; process_times[0]=0;priority[0]=0;
		for(int q=0;q<n_skill;q++) {
			task_skill[0][q] = 0;
		}
		int current_task = 1;
		for(Task task: whole_tasks) {
			arrival_times[current_task] = task.arrival_time;
			process_times[current_task] = task.process_time;
			priority[current_task] = task.priority;
			for(int q=0;q<n_skill;q++) {
				task_skill[current_task][q] = task.skill_req[q];
			}
			current_task ++;
		}
		
		int[][] team_skill = new int[problem.teams.length][n_skill];
		int current_team = 0;
		for(Team team: problem.teams) {
			for(int q=0;q<n_skill;q++) {
				team_skill[current_team][q] = team.skills_have[q];
			}
			current_team ++;
		}
		
		int[][] travel_time = new int[n_task+1][n_task+1];
		travel_time[0][0] = problem.travel_times[0][0];
		for(int i=1;i<n_task+1;i++) {
			Task task = index_task_map.get(i);
			travel_time[0][i] = problem.travel_times[0][task.taskID];
			travel_time[i][0] = problem.travel_times[task.taskID][0];
		}
		for(int i=1;i<n_task+1;i++) {
			for(int j=1;j<n_task+1;j++) {
				Task t1 = index_task_map.get(i);
				Task t2 = index_task_map.get(j);
				travel_time[i][j] = problem.travel_times[t1.taskID][t2.taskID];
			}
		}
		int[][] dummy_travel = new int[n_team][n_task+1];
		if(n_period==0) {
			for(int k=0;k<n_team;k++) {
				for(int i=0;i<n_task+1;i++) {
					dummy_travel[k][i] = travel_time[0][i];
				}
			}
		}else {
			for(int k=0;k<n_team;k++) {
				Task task = starting_nodes[k];
				for(int i=0;i<n_task+1;i++) {
					dummy_travel[k][i] = problem.travel_times[task.taskID][i];
				}
			}
		}
		
		PeriodMIP period_mip = new PeriodMIP(n_task, n_team, n_skill, starting_times, arrival_times, process_times, priority, 
											 t_now, t_end, travel_time, task_skill, team_skill, dummy_travel);

	    return period_mip;
	}
	
	public ArrayList<Task> run(Problem problem, HashMap<Task, Integer> completed, int[] starting_times, Task[] starting_nodes, ArrayList<Task> residual, 
							   int t_end, int frozen_len, boolean cont, int next_t_now) throws GRBException{
		ArrayList<Task> N2 = new ArrayList<Task>();  // will return
		
		PeriodMIP period_mip = convert(problem, starting_times, starting_nodes, residual, t_end);
		
		String model_name = "Period " + n_period;
		MIP mip = new MIP(model_name, period_mip);
		mip.run();
		
		t_now = next_t_now;
		
		int[] completion_times = mip.get_C();
		int[] o_vals = mip.get_O();
		for(int i=1;i<completion_times.length;i++) {
			if (completion_times[i] <= t_now + frozen_len) {
				Task task = whole_tasks.get(i-1);
				if(!completed.containsKey(task)) {
					completed.put(task, completion_times[i]);
				}
			}
		}
		for(int i=1;i<o_vals.length;i++) {
			if(o_vals[i]==1) {
				Task task = whole_tasks.get(i-1);
				if(!completed.containsKey(task)) {
					completed.put(task, completion_times[i]);
				}
			}
		}
		if(cont) {
			
			HashMap<Integer, Task> index_task_map = new HashMap<Integer, Task>();
			HashMap<Task, Integer> task_index_map = new HashMap<Task, Integer>();
			
			int temp_count = 1;
			for(Task t: whole_tasks) {
				index_task_map.put(temp_count, t);
				task_index_map.put(t, temp_count);
				temp_count ++;
			}
			
			ArrayList<ArrayList<Integer>> routes = mip.routes;
			
			for(int k=0;k<problem.teams.length;k++) {
				ArrayList<Integer> route = routes.get(k);
				int last_index = 1;
				for(int i=1;i<route.size();i++) {
					int node = route.get(i);
					int c_t = completion_times[node];
					if(c_t <= t_now + frozen_len) {
						last_index = node;
					}else {
						Task task = index_task_map.get(node);
						if(!N2.contains(task)) {
							N2.add(task);
						}	
					}
				}
				Task last_task = index_task_map.get(last_index);
				starting_nodes[k] = last_task;
				if(completion_times[last_index] > t_now) {
					starting_times[k]= completion_times[last_index];
				}else {
					starting_times[k]= t_now;
				}
			}
		}
		System.out.println("Size of N2: " + N2.size());
		return N2;
	}
	
}
