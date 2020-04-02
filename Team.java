package mip;

public class Team {

	int teamID;
	int[] skills_have;
	
	public Team(int teamID, int[] skills_have) {
		this.teamID = teamID;
		this.skills_have = skills_have;
	}
	
	public void display() {
		System.out.println("Team: " + teamID);
		System.out.println("----------------");
		for(int i=0;i<skills_have.length;i++) {
			System.out.println("Skill " + (i+1) +": " + skills_have[i]);
		}
		System.out.println();
	}
}
