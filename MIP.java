package mip;

import java.util.ArrayList;

import gurobi.*;

public class MIP {
	
	PeriodMIP period;  // It includes the problem sets and parameters
	
	// Gurobi environment and the model
	GRBEnv env;
	GRBModel model;
		
	// Decision variables
	GRBVar[][][] X;
	GRBVar[][] Y;
	GRBVar[] O;
	GRBVar[][] D;
	GRBVar[] C;
	
	// Objective Function Value
	Double obj_val;
	
	// Routes
	ArrayList<ArrayList<Integer>> routes;
	
	public MIP(String problem_name, PeriodMIP period) throws GRBException {
		
		// Construction method
		
		this.period = period;
		env = new GRBEnv();
		model = new GRBModel(env);
		model.set(GRB.StringAttr.ModelName, problem_name);
		
		routes = new ArrayList<ArrayList<Integer>>();
	}
	
	public void set_decision_variables() throws GRBException {
		
		/*
		 * This method defines the decision variables of the model.
		 * N_prime is introduced as a set of nodes to all of them.
		 * If a decision variable is valid for the set N, we will
		 * handle this issue in the process of developing relevant
		 * constraints. 
		 * 
		 * For example, in our case, value of Y[0][k] becomes 
		 * unimportant
		 */
		
		// X_ijk, 
		X = new GRBVar[period.n_task+1][period.n_task+1][period.n_team];
		for(Integer i: period.N_prime) {
			for(Integer j: period.N_prime) {
				for(Integer k: period.K) {
					if(i == j) {
						X[i][j][k] = model.addVar(0, 0, 0, GRB.BINARY, "X"+"_"+i+"_"+j+"_"+k);
					}else {
						X[i][j][k] = model.addVar(0, 1, 0, GRB.BINARY, "X"+"_"+i+"_"+j+"_"+k);
					}
				}
			}
		}
		
		// Y_ik
		Y = new GRBVar[period.n_task+1][period.n_team];
		for(Integer i: period.N_prime) {
			for(Integer k: period.K) {
				Y[i][k] = model.addVar(0, 1, 0, GRB.BINARY, "Y_"+ i + "_" + k);
			}
		}
		
		// O_i
		O = new GRBVar[period.n_task+1];
		for(Integer i: period.N_prime) {
			O[i] = model.addVar(0, 1, 0, GRB.BINARY, "O_"+i); // O(0) is not important
		}
				
		// D_ik
		D = new GRBVar[period.n_task+1][period.n_team];
		for(Integer i: period.N_prime) {
			for(Integer k: period.K) {
				D[i][k] = model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS, "D_"+ i + "_" + k);
			}
		}
		
		// C_i
		C = new GRBVar[period.n_task+1];
		for(Integer i: period.N_prime) {
			C[i] = model.addVar(0, GRB.INFINITY, period.priority[i], GRB.CONTINUOUS, "C_"+i); // C(0) is not important
		}
		
	}
	
	public void must_be_completed() throws  GRBException {
		
		// O_i + sum(k)Y_ik >= 1, for all i in real tasks
		for(Integer i: period.N) {
			GRBLinExpr lhs = new GRBLinExpr();
			for(Integer k: period.K) {
				lhs.addTerm(1.0, Y[i][k]);
			}
			lhs.addTerm(1.0, O[i]);
			model.addConstr(lhs, GRB.GREATER_EQUAL, 1, "Completion_"+i);
		}
	}
	
	public void either_outsourcing_or_team() throws GRBException {
		
		// sum(k)Y_ik <= |K|(1-O_i) for all i in real tasks
		for(Integer i: period.N) {
			GRBLinExpr lhs = new GRBLinExpr();
			for(Integer k: period.K) {
				lhs.addTerm(1.0, Y[i][k]);
			}
			GRBLinExpr rhs = new GRBLinExpr();
			rhs.addTerm(-1*period.n_team, O[i]);
			rhs.addConstant(period.n_team);
			model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "Either_"+i);
		}
	}
	
	public void startings() throws GRBException {
		
		// starting place and time of teams
		
		for(Integer k: period.K) {
			model.addConstr(Y[0][k], GRB.EQUAL, 1, "StartNode_"+k);  // Y_0k =1 for each k in teams
			model.addConstr(D[0][k], GRB.EQUAL, period.starting_times[k], "StartTime_"+k);  //D_0k for each k in teams
		}
	}
	
	public void to_depot() throws GRBException {
		
		// sum(i in N) X_i0k = 1 for all k in teams
		for(Integer k: period.K) {
			GRBLinExpr lhs = new GRBLinExpr();
			for(Integer i: period.N) {
				lhs.addTerm(1.0, X[i][0][k]);
			}
			model.addConstr(lhs, GRB.EQUAL, 1, "toDepot_"+k);
		}
	}

	public void X_Y_relation() throws GRBException {
		// sum(j in N:i!=j) X_ijk = Y_ik, for all i in tasks for all k in teams
		for(Integer i: period.N_prime) {
			for(Integer k: period.K) {
				GRBLinExpr lhs = new GRBLinExpr();
				for(Integer j: period.N_prime) {
					if(i != j) {
						lhs.addTerm(1.0, X[i][j][k]);
					}
				}
				model.addConstr(lhs, GRB.EQUAL, Y[i][k], "XY_"+i+"_"+k);
			}
		}
	}
	
	public void flow_balance() throws GRBException {
		
		// sum(j in N:j!=i)X_jik = sum(j in N:j!=i)X_ijk
		// flow balance
		for(Integer i: period.N) {
			for(Integer k: period.K) {
				GRBLinExpr lhs = new GRBLinExpr();
				for(Integer j: period.N_prime) {
					if (i!=j) {
						lhs.addTerm(1.0, X[j][i][k]);
					}
				}
				GRBLinExpr rhs = new GRBLinExpr();
				for(Integer j: period.N_prime) {
					if (i!=j) {
						rhs.addTerm(1.0, X[i][j][k]);
					}
				}
				model.addConstr(lhs, GRB.EQUAL, rhs, "FlowBalance_"+i+"_"+k);
			}
		}
	}
	
	public void scheduling() throws GRBException {
		
		// C_i + t_ij + p_j <= D_jk + (day_end)*(1-X[i][j][k])  for all i,j in tasks i!=j for all k in teams
		for(Integer i: period.N_prime) {
			for(Integer j: period.N) {
				if (i!=j) {
					
					if (i > 0) {
						for(Integer k: period.K) {
							GRBLinExpr lhs = new GRBLinExpr();
							lhs.addTerm(1.0, D[i][k]);
							lhs.addConstant(period.travel_times[i][j] + period.process_times[j]);
							GRBLinExpr rhs = new GRBLinExpr();
							rhs.addTerm(1.0, D[j][k]);
							rhs.addTerm(-1*period.t_end, X[i][j][k]);
							rhs.addConstant(period.t_end);
							model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "Scheduling_"+i+"_"+j+"_"+k);
						}
					}
					else {
						for(Integer k: period.K) {
							GRBLinExpr lhs = new GRBLinExpr();
							lhs.addTerm(1.0, D[i][k]);
							lhs.addConstant(period.dummy_travel[k][j] + period.process_times[j]);
							GRBLinExpr rhs = new GRBLinExpr();
							rhs.addTerm(1.0, D[j][k]);
							rhs.addTerm(-1*period.t_end, X[i][j][k]);
							rhs.addConstant(period.t_end);
							model.addConstr(lhs, GRB.LESS_EQUAL, rhs, "Scheduling_"+i+"_"+j+"_"+k);
						}
					}
					
				}
			}
		}
	}
	
	public void synchronization() throws GRBException {
		
		// D_ik <= C_i
		// D_ik + M(1-Y_ik) >= C_i - MO_i forall i in N, k in K
		for(Integer i: period.N_prime) {
			for(Integer k: period.K) {
				model.addConstr(D[i][k], GRB.LESS_EQUAL, C[i], "Synch1"+i+"_"+k);
				// Sycnh2
				GRBLinExpr lhs = new GRBLinExpr();
				lhs.addTerm(1.0, D[i][k]);
				lhs.addTerm(-1*period.t_end, Y[i][k]);
				lhs.addConstant(period.t_end);
				GRBLinExpr rhs = new GRBLinExpr();
				rhs.addTerm(1.0, C[i]);
				rhs.addTerm(-1*period.t_end, O[i]);
				model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, "Synch2"+i+"_"+"_"+k);
			}
		}
	}
	
	public void test() throws GRBException {
		
		for(Integer i: period.N_prime) {
			for(Integer k: period.K) {
				GRBLinExpr rhs = new GRBLinExpr();
				rhs.addTerm(540, Y[i][k]);
				model.addConstr(D[i][k], GRB.LESS_EQUAL, rhs, "test"+"_i"+"k");		
			}
		}
	}
	
	public void outsourced_tasks() throws GRBException {
		
		for(Integer i: period.N) {
			// Constraint1
			GRBLinExpr rhs = new GRBLinExpr();
			rhs.addTerm(period.t_end, O[i]);
			rhs.addConstant(period.t_end);
			rhs.addTerm(-1*period.t_end, O[i]);
			model.addConstr(C[i], GRB.LESS_EQUAL, rhs, "out1_"+i);
			// Constraint2
			GRBLinExpr rhs2 = new GRBLinExpr();
			rhs2.addTerm(period.t_end, O[i]);
			rhs2.addConstant(period.t_current+period.process_times[i]);
			rhs2.addTerm(-1*(period.t_current + period.process_times[i]), O[i]);
			model.addConstr(C[i], GRB.GREATER_EQUAL, rhs2, "out2_"+i);
		}
	}
	
	public void matching() throws GRBException {
		
		for(Integer i: period.N_prime) {
			for(Integer q: period.Q) {
				GRBLinExpr lhs = new GRBLinExpr();
				for(Integer k: period.K) {
					lhs.addTerm(period.team_skill[k][q], Y[i][k]);
				}
				GRBLinExpr rhs = new GRBLinExpr();
				rhs.addTerm(-1*period.n_team, O[i]);
				rhs.addConstant(period.task_skill[i][q]);
				model.addConstr(lhs, GRB.GREATER_EQUAL, rhs, "Matching_"+i+"_"+q);
			}
		}
	}
	
	public int[][][] get_X() throws GRBException {
		
		int [][][] x_val = new int[period.n_task+1][period.n_task+1][period.n_team];
		for(Integer i: period.N_prime) {
			for(Integer j: period.N_prime) {
				for(Integer k: period.K) {
					x_val[i][j][k] = (int) X[i][j][k].get(GRB.DoubleAttr.X);
				}
			}
		}
		return x_val;
	}
	
	public int[] get_C() throws GRBException {
		
		int[] c_val = new int[period.n_task + 1];
		for(Integer i: period.N_prime) {
			c_val[i] = (int) Math.round(C[i].get(GRB.DoubleAttr.X));
		}
		return c_val;
	}
	
	public int[] get_O() throws GRBException {
		
		int[] o_val = new int[period.n_task + 1];
		for(Integer i: period.N_prime) {
			if (O[i].get(GRB.DoubleAttr.X) > 0.1) {
				o_val[i] = 1;
			}else {
				o_val[i] = 0;
			}
		}
		return o_val;
	}
	
	public void display_nonzero_variables() throws GRBException {
		
		GRBVar[] vars = model.getVars();
	    for (int i = 0; i < model.get(GRB.IntAttr.NumVars); ++i) {
	    	GRBVar sv = vars[i];
	        if (sv.get(GRB.DoubleAttr.X) > 1e-6) {
	        	System.out.println(sv.get(GRB.StringAttr.VarName) + " = " +
	              sv.get(GRB.DoubleAttr.X));
	        }
	    }
	}
	
	public void set_routes() throws GRBException {
		for(Integer k: period.K) {
			ArrayList<Integer> route = new ArrayList<Integer>();
			int last_node = 0;
			route.add(last_node);
			boolean flag = true;
			while(flag) {
				for(Integer j:period.N_prime) {
					if(X[last_node][j][k].get(GRB.DoubleAttr.X) > 0.1) {
						last_node = j;
						if (last_node != 0) {
							route.add(last_node);
							break;
						}else {
							flag = false;
							break;
						}
					}
				}
			}
			routes.add(route);
		}
	}
	
	public void display_routes() {
		
		System.out.println("Routes: ");
		System.out.println("-------");
		System.out.println();
		for(Integer k:period.K) {
			System.out.print("Team " + k + ": ");
			ArrayList<Integer> route = routes.get(k);
			for(int i=0;i<route.size();i++) {
				System.out.print(route.get(i)+"_");
			}
			System.out.println();
			System.out.println();
		}
	}
	
	public void run() throws GRBException {
		
		set_decision_variables();
		must_be_completed();
		either_outsourcing_or_team();
		startings();
		to_depot();
		X_Y_relation();
		flow_balance();
		scheduling();
		synchronization();
		outsourced_tasks();
		matching();
		test();
		model.set(GRB.IntAttr.ModelSense, GRB.MINIMIZE);
		model.optimize();
		int status = model.get(GRB.IntAttr.Status);
		if (status == GRB.Status.OPTIMAL) {
			obj_val = model.get(GRB.DoubleAttr.ObjVal);
	        System.out.println("The optimal objective is " + obj_val);
	        display_nonzero_variables();
	        set_routes();
	        display_routes();
		}
	}

}
