package mip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

public class Problem {
	
	String excelFilePath;
	FileInputStream inputStream;
	Workbook workbook;
	
	Task[] tasks;
	Team[] teams;
	int[][] travel_times;
	
	ArrayList<ArrayList<Task>> task_which_period;
	
	public Problem(String excel_file_path){
		
		task_which_period = new ArrayList<ArrayList<Task>>();
		 excelFilePath = excel_file_path; 
		 try {
			inputStream = new FileInputStream(new File(excelFilePath));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		 try {
		     workbook = new XSSFWorkbook(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	 }
	
	public void read_teams() {
		
		Sheet sheet = workbook.getSheetAt(2);
		int current_row=0, total_column=0, total_row=0;
		for (Row row: sheet) {
			if(current_row>0) {
				total_row ++;
			}
			else {
				for(Cell cell: row) {
					total_column ++;
				}
			}
			current_row ++;
		}
		int total_skill = total_column - 1;
		teams = new Team[total_row];
		int n_row = 0;
		for (Row row: sheet) {
			int teamID = -1;
			int[] skill_have = new int[total_skill];
			if(n_row > 0) {
				int n_column = 0;
				for(Cell cell: row) {
					if(n_column == 0) {
                		teamID = (int) cell.getNumericCellValue();
                		
                	}else {
                		skill_have[n_column-1] = (int) cell.getNumericCellValue();
                	}
					n_column ++;
				}
				Team team_ = new Team(teamID, skill_have);
				teams[n_row-1] = team_;
			}
			n_row ++;
		}
	}
	
	public void read_tasks() {
		
		Sheet sheet = workbook.getSheetAt(0);
		int current_row=0, total_column=0, total_row=0;
		for (Row row: sheet) {
			if(current_row>0) {
				total_row ++;
			}
			else {
				for(Cell cell: row) {
					total_column ++;
				}
			}
			current_row ++;
		}
		int total_skill = total_column - 4;
		tasks = new Task[total_row];
		int n_row = 0;	
        for (Row row: sheet) {
        	int taskID=-1;
        	int arrival_time=-1;
        	int process_time=-1;
        	int priority=-1;
        	int[] skill_req = new int[total_skill];
        	if (n_row > 0) 
        	{
        		int n_column = 0;
                for(Cell cell: row) {
                	if(n_column == 0) {
                		taskID = (int) cell.getNumericCellValue();
                	}else if(n_column == 1) {
                		arrival_time = (int) cell.getNumericCellValue(); 
                	}else if(n_column == 2) {
                		process_time = (int) cell.getNumericCellValue(); 
                	}else if(n_column == 3) {
                		priority = (int) cell.getNumericCellValue(); 
                	}else {
                		skill_req[n_column-4] = (int) cell.getNumericCellValue(); 
                	}
                	n_column ++;
                }
                Task task_ = new Task(taskID, arrival_time, process_time, priority, skill_req);
            	tasks[n_row-1] = task_;
        	}	
        	n_row ++;
        }
	}
	
	public void read_travel_times(int k){
		
		travel_times = new int[k][k];
		XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(1); // which sheet in excel
		Iterator<Row> iterator = sheet.iterator(); // create row iterator
		for (int i = 0; iterator.hasNext(); i++) {
			Row nextRow = iterator.next(); 
			Iterator<Cell> cellIterator = nextRow.cellIterator(); // create cell
			if(i>=1){
				for (int j = 0; cellIterator.hasNext(); j++) { 
					Cell cell = cellIterator.next();
					if(j>=1){
						travel_times[i-1][j-1]=(int)cell.getNumericCellValue();
					}
				}
			}
		}
	}
	
	public void read_all() {
		read_tasks();
		read_teams();
		int k = tasks.length;
		read_travel_times(k+1);
	}

	public void seperate_tasks(int period_limit) {
		int n_assigned = 0;
		ArrayList<Task> static_tasks = new ArrayList<Task>();
		ArrayList<Task> dynamic_tasks = new ArrayList<Task>();
		for(Task task: tasks) {
			if(task.arrival_time == 0) {
				static_tasks.add(task);
				n_assigned ++;
			}else {
				dynamic_tasks.add(task);
			}
		}
		task_which_period.add(static_tasks);
		
		int n_in_period = 0;
		int n_seperated_assign = static_tasks.size();
		ArrayList<Task> curr_ = new ArrayList<Task>();
		while(n_assigned < tasks.length) {
			if(n_in_period == period_limit) {
				task_which_period.add(curr_);
				n_in_period = 0;
				curr_ = new ArrayList<Task>();
				n_seperated_assign += period_limit;
			}
			Task f = dynamic_tasks.get(0);
			curr_.add(f);
			dynamic_tasks.remove(f);
			n_assigned ++;
			n_in_period ++;
		}
		
		ArrayList<Task> temp = new ArrayList<Task>();
		for(int i=n_seperated_assign; i<tasks.length;i++) {
			temp.add(tasks[i]);
		}
		task_which_period.add(temp);
		
	}

	public void period_displays() {
		
		System.out.println("Number of tasks appeared at the beginning of day: ");
		System.out.println("------------------------------------------------- ");
		ArrayList<Task> static_tasks = task_which_period.get(0);
		for(Task task: static_tasks) {
			System.out.print(task.taskID+"_");
		}
		System.out.println();
		System.out.println();
		for(int i=1;i<task_which_period.size();i++) {
			ArrayList<Task> temp_tasks = task_which_period.get(i);
			System.out.println("Number of tasks appeared in dynamic period " + i + ": ");
			System.out.println("-----------------------------------------------------");
			for(Task task: temp_tasks) {
				System.out.print(task.taskID+"_");
			}
			System.out.println();
			System.out.println();
		}
	}

	public int get_period_num() {
		
		// including static period
		return task_which_period.size();
	}
	
	public ArrayList<Task> get_period_tasks(int which_period){
		
		//  0 for static period, then gradually increments for the dynamic periods
		return task_which_period.get(which_period);
	}


}
