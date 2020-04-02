package mip;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import gurobi.GRBException;

public class RunScript {
	
	public static void write_solution(HashMap<Task, Integer> completed) throws IOException {
		
		Workbook wb = new XSSFWorkbook();
		XSSFSheet sheet = (XSSFSheet) wb.createSheet("Completion Times");
		
		Set<Task> keys = completed.keySet();
		Task[] keyOf = keys.toArray(new Task[keys.size()]);
		
		Row header = sheet.createRow(0);
		Cell cell1 = header.createCell(0);
		cell1.setCellValue("TaskID");
		cell1 = header.createCell(1);
		cell1.setCellValue("Priority");
		cell1 = header.createCell(2);
		cell1.setCellValue("Completion Time");
		for(int i=0;i<keyOf.length;i++){
			Row row = sheet.createRow(i+1);
			Cell cell = row.createCell(0);
			cell.setCellValue(keyOf[i].taskID);
			cell = row.createCell(1);
			cell.setCellValue(keyOf[i].priority);
			cell = row.createCell(2);
			cell.setCellValue(completed.get(keyOf[i]));
		}
		
		FileOutputStream fileOut = new FileOutputStream("Results.xlsx");
		wb.write(fileOut);
		fileOut.close();
		wb.close();
	}
	
	public static void solve_problem(String file_path, int period_task_limit, int t_end, int frozen_len) throws GRBException, IOException {
		
		HashMap<Task, Integer> completed = new HashMap<Task, Integer>();
		
		// ************************************ READ & PREPROCESS ************************************************
		
		Problem problem = new Problem(file_path);
		problem.read_all();
		problem.seperate_tasks(period_task_limit);
		int total_num_period = problem.task_which_period.size();
		PeriodFramework[] period = new PeriodFramework[total_num_period];
		for(int i=0;i<total_num_period;i++) {
			period[i] = new PeriodFramework(i, problem.get_period_tasks(i));
		}
		
		// ************************************ RUN FOR ALL PERIODS ************************************************
		
		ArrayList<Task> residual = new ArrayList<Task>();
		int[] starting_times = new int[problem.teams.length];
		Task[] starting_task = new Task[problem.teams.length];
		for(int k=0;k<problem.teams.length;k++) {
			starting_times[k]=0;
			starting_task[k] = problem.tasks[0];
		}
		boolean flag = true;
		for(int p=0;p<period.length;p++) {
			int next_t_now = -1; 
			if(p<period.length-1) {
				next_t_now = period[p+1].t_now;
			}else {
				next_t_now = 10000;
				flag=false;
			}
			residual = period[p].run(problem, completed, starting_times, starting_task, residual, t_end, frozen_len, flag, next_t_now);
			System.out.println("Completed size: " + completed.size());
		}
		
		write_solution(completed);
	}

	public static void main(String[] args) throws GRBException, IOException {
		
		// ************************************* PROBLEM PARAMETERS **********************************************
		
		String file_path = "C:/Users/ODEMIRAY18/eclipse-workspace/Tubitak_MIP/data/test_data.xlsx";
		int period_task_limit = 5;
		int t_end = 540;
		int frozen_len = 10;
		
		solve_problem(file_path, period_task_limit, t_end, frozen_len);
		
		System.out.println("Exit 0");
		
	}

}
