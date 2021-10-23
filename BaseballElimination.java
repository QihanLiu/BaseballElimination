
/**
 *
 * @author Qihan Liu
 * 
 * 
 **/
import edu.princeton.cs.algs4.StdOut;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FlowEdge;
import java.util.HashMap;
import java.util.Arrays;
import java.util.LinkedList;

public class BaseballElimination {
    // current max win
    private int maxWin;
    // list of teams that have max win
    private LinkedList<String> maxWinTeams;
    // total teams
    private final int numOfTeams;
    private final int[][] remainGames;
    private final String[] teamNames;
    // teams info
    private final HashMap<String, Team> teams;
    // subset R of teams
    private LinkedList<String> subsetR;
    // subsetR is belong to
    private String subsetROf;

    // create a baseball division from given filename in format specified below
    public BaseballElimination(String filename) {
        In file = new In(filename);
        numOfTeams = Integer.parseInt(file.readLine());
        maxWinTeams = new LinkedList<String>();
        teams = new HashMap<String, Team>(numOfTeams);
        remainGames = new int[numOfTeams][numOfTeams];
        teamNames = new String[numOfTeams];
        // construct teams
        String name;
        for (int i = 0; i < numOfTeams; i++) {
            name = file.readString();
            teams.put(name, new Team(file, i, name));
            teamNames[i] = name;
        }
        subsetR = null;
    }

    // Team info
    private class Team {
        private final int teamID;
        private final int numWin;
        private final int numLose;
        private final int numLeft;

        // team info
        private Team(In file, int i, String name) {
            teamID = i;
            numWin = file.readInt();
            numLose = file.readInt();
            numLeft = file.readInt();
            for (int j = 0; j < numOfTeams; j++) {
                remainGames[i][j] = file.readInt();
            }
            if (maxWin < numWin) {
                maxWin = numWin;
                maxWinTeams = new LinkedList<String>();
                maxWinTeams.add(name);
            }
            else if (maxWin == numWin) {
                maxWinTeams.add(name);
            }
        }
    }

    // number of teams
    public int numberOfTeams() {
        return teams.size();
    }

    // all teams
    public Iterable<String> teams() {
        return Arrays.asList(teamNames);
    }

    // number of wins for given team
    public int wins(String team) {
        checkTeam(team);
        return teams.get(team).numWin;
    }

    // number of losses for given team
    public int losses(String team) {
        checkTeam(team);
        return teams.get(team).numLose;
    }

    // number of remaining games for given team
    public int remaining(String team) {
        checkTeam(team);
        return teams.get(team).numLeft;
    }

    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        checkTeam(team1);
        checkTeam(team2);
        int ind1, ind2;
        ind1 = teams.get(team1).teamID;
        ind2 = teams.get(team2).teamID;
        return remainGames[ind1][ind2];
    }

    // is given team eliminated?
    public boolean isEliminated(String team) {
        checkTeam(team);
        if (numOfTeams == 1) {
            return false;
        }
        subsetROf = team;
        subsetR = null;
        Team thisTeam = teams.get(team);
        // if it is trivial, return max win team
        if (trivialLose(thisTeam)) {
            subsetR = maxWinTeams;
            return true;
        }
        return noTrivialLose(team);
    }

    private boolean noTrivialLose(String team) {
        Team ignoreTeam = teams.get(team);
        int ignoredTeamID = ignoreTeam.teamID;
        int bestWin = ignoreTeam.numWin + ignoreTeam.numLeft;
        // total games pair
        int numOfPairs = (numOfTeams - 1) * (numOfTeams - 2) / 2;
        FlowNetwork teamnetNetwork = new FlowNetwork(numOfPairs + (numOfTeams - 1) + 2);
        // index of s and t
        int s = numOfPairs + (numOfTeams - 1), t = numOfPairs + (numOfTeams - 1) + 1;

        int gameVertice = numOfTeams - 1, teamVertice, limitWin, totRemainGames = 0;
        // team1 is the i in g[i,j]
        // go through all the remaining games
        for (int i = 0; i < numOfTeams; i++) {
            if (i == ignoredTeamID) {
                continue;
            }
            // capasity of i to t
            limitWin = bestWin - teams.get(teamNames[i]).numWin;
            // index of team1 in the graph
            if (i < ignoredTeamID)
                teamVertice = i;
            else
                teamVertice = i - 1;
            // add team vertice to end
            teamnetNetwork.addEdge(new FlowEdge(teamVertice, t, limitWin));
            // team2 is the j in g[i,j], we go through all the opponents of i
            for (int j = i + 1; j < numOfTeams; j++) {
                if (j == ignoredTeamID) {
                    continue;
                }
                totRemainGames += remainGames[i][j];
                // add edge to source
                teamnetNetwork.addEdge(new FlowEdge(s, gameVertice, remainGames[i][j]));
                // index of team1 in the graph
                if (i < ignoredTeamID)
                    teamVertice = i;
                else
                    teamVertice = i - 1;
                teamnetNetwork.addEdge(new FlowEdge(gameVertice, teamVertice, Double.POSITIVE_INFINITY));
                // index of j in the graph
                if (j < ignoredTeamID)
                    teamVertice = j;
                else
                    teamVertice = j - 1;
                teamnetNetwork.addEdge(new FlowEdge(gameVertice, teamVertice, Double.POSITIVE_INFINITY));
                gameVertice++;
            }
        }

        FordFulkerson teamGraph = new FordFulkerson(teamnetNetwork, s, t);
        // Find subsetR of the graph
        if (teamGraph.value() == totRemainGames) {
            return false;
        }
        subsetR = new LinkedList<String>();
            for (int i = 0; i < numOfTeams; i++) {
                if (i == ignoredTeamID) {
                    continue;
                }
                if (i < ignoredTeamID)
                    teamVertice = i;
                else
                    teamVertice = i - 1;
                if (teamGraph.inCut(teamVertice)) {
                    subsetR.add(teamNames[i]);
                }
            }
        subsetROf = team;

        return true;
    }

    private boolean trivialLose(Team thisTeam) {
        if (thisTeam.numWin + thisTeam.numLeft < maxWin) {
            return true;
        }
        return false;
    }

    private void checkTeam(String team) {
        if (!teams.containsKey(team)) {
            throw new IllegalArgumentException("Team " + team + " is not in the list");
        }
    }

    // // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        // retrieve record if the subset is already calculated.
        if (team.equals(subsetROf)) {
            return subsetR;
        }
        // if it is a new input, construct the graph.
        isEliminated(team);
        return subsetR;
    }

    // unit testing (optional)
    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination("teams5.txt");
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                StdOut.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    StdOut.print(t + " ");
                }
                StdOut.println("}");
            } else {
                StdOut.println(team + " is not eliminated");
            }
            StdOut.println(division.certificateOfElimination(team));
        }
    }
}