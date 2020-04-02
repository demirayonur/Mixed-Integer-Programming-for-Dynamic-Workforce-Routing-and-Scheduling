package mip;

public class Task {

	int taskID;
	int arrival_time;
	int process_time;
	int priority;
	int[] skill_req;
	
	public Task(int taskID, int arrival_time, int process_time, int priority, int[] skill_req) {
		this.taskID = taskID;
		this.arrival_time = arrival_time;
		this.process_time = process_time;
		this.priority = priority;
		this.skill_req = skill_req;
	}
	
	public void display() {
		System.out.println("Task: " + taskID);
		System.out.println("----------------");
		System.out.println("Arrival Time: " + arrival_time);
		System.out.println("Process Time: " + process_time);
		System.out.println("Priority    : " + priority);
		for(int i=0;i<skill_req.length;i++) {
			System.out.println("Skill " + (i+1) +": " + skill_req[i]);
		}
		System.out.println();
	}
}
