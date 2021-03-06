package com.delawareparkvolleyball.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import com.mysql.jdbc.Driver ; 

import com.delawareparkvolleyball.schedule.DayOfTheWeek;
import com.delawareparkvolleyball.schedule.League;
import com.delawareparkvolleyball.schedule.MatchResult;
import com.delawareparkvolleyball.schedule.Person;
import com.delawareparkvolleyball.schedule.Schedule;
import com.delawareparkvolleyball.schedule.Team;
import com.delawareparkvolleyball.view.HtmlGeneration;

public class MySqlReadWriteUpdate {

	private static Connection mysqlConnection = null;

	private static Connection fetchConnection() {
		try {
			if (mysqlConnection != null && !mysqlConnection.isClosed()) {
				return mysqlConnection;
			} else {
				Class.forName("com.mysql.jdbc.Driver");
				String url = "jdbc:mysql://localhost:3306/dpva";
				String username = "dev";
				String password = "dddddd";
				mysqlConnection = DriverManager.getConnection(url, username,
						password);
			}
		} catch (ClassNotFoundException classNotFoundException) {
			System.out.println("Unable to create connection in MySqlReadWriteUpdate. ClassNotFoundException: "
					+ classNotFoundException.getMessage());
		} catch (SQLException exception) {
			System.out.println("SQL Error when fetching connection: "
					+ exception.getMessage());
		}
		return mysqlConnection;
	}

	public static String htmlOfStandings(int leagueId) {
		League league = fetchLeague(leagueId);
		ArrayList<Team> allTeams = league.getAllTeams();
		ArrayList<MatchResult> allMatchResults = fetchAllResults(leagueId, league) ; 
		String html = "<table border=\"1\" style=\"border-collapse:collapse; border-color:#884444\" >\n";
		html += HtmlGeneration.generateStandingsHeader(null) ;
		int teamCount = allTeams.size();
		for (int i = 0; i < teamCount; i++) {
			Team team = allTeams.get(i) ; 
			int[] winsAndLosses = computeWinsAndLosses(team, allMatchResults) ; 
			if (i % 2 == 0) {
				html += "\t<tr bgcolor=\"#ffff88\">\n";
			} else {
				html += "\t<tr>\n";
			}

			// html += "\t<tr> " ; //TODO: use datatables.net jquery plugin
			html += "\t\t<td>" + allTeams.get(i).getWoman().getFirstName()
					+ "</td>";
			html += "\t\t<td>" + allTeams.get(i).getMan().getFirstName()
					+ "</td>";
			html += "\t\t<td>" + winsAndLosses[0] 
					+ "</td>";
			html += "\t\t<td>" + winsAndLosses[1] 
					+ "</td>";
			
			html += "\t</tr> ";
		}
		html += "</table>";
		return html;
	}

	/** 
	 * Returns wins or losses - for now just one date.
	 * @param team
	 * @param allMatchResults
	 * @return
	 */
	private static int[] computeWinsAndLosses(Team team,
			ArrayList<MatchResult> allMatchResults) {
		int[] winsAndLosses = new int[2] ; // 0 is wins, 1 is losses.
		for (Iterator<MatchResult> iterator = allMatchResults.iterator(); iterator.hasNext();) {
			MatchResult matchResult = (MatchResult) iterator.next();
			String containsTeam = matchResult.containsTeam(team) ; 
			if("A".equalsIgnoreCase(containsTeam)) {
				winsAndLosses[0] += matchResult.getTeamAwins() ;
				winsAndLosses[1] += matchResult.getTeamBwins() ;
			}
			else if ("B".equalsIgnoreCase(containsTeam)) {
				winsAndLosses[0] += matchResult.getTeamBwins() ;
				winsAndLosses[1] += matchResult.getTeamAwins() ;
			}
		}
		return winsAndLosses ;
	}

	public static ArrayList<MatchResult> fetchAllResults(int leagueId, League league) {
		ArrayList<MatchResult> allMatches = new ArrayList<MatchResult>() ; 
		Connection connection = fetchConnection();
		try {
			// Now get all the teams;
			String selectMatchsSql = " select mr.team_A_wins, mr.team_B_wins, mr.play_date,  ";
			selectMatchsSql += " apf.first_name, apf.last_name, apm.first_name, apm.last_name, ";
			selectMatchsSql += " bpf.first_name, bpf.last_name, bpm.first_name, bpm.last_name ";
			selectMatchsSql += " from dpva.match_result mr ";
			selectMatchsSql += " join team ta on ta.id = mr.team_A_id ";
			selectMatchsSql += " join person apf on ta.woman_id = apf.id ";
			selectMatchsSql += " join person apm on ta.man_id = apm.id ";
			selectMatchsSql += " join team tb on tb.id = mr.team_B_id ";
			selectMatchsSql += " join person bpf on tb.woman_id = bpf.id ";
			selectMatchsSql += " join person bpm on tb.man_id = bpm.id ";
			selectMatchsSql += " where mr.league_id = " + leagueId + " ;";
			Statement matchesStatement = connection.createStatement();
			ResultSet matchesResultSet = matchesStatement.executeQuery(selectMatchsSql);
			while (matchesResultSet.next()) {
				Date date = matchesResultSet.getDate("mr.play_date") ;
				int teamAwins = matchesResultSet.getInt("mr.team_A_wins") ;
				int teamBwins = matchesResultSet.getInt("mr.team_B_wins") ;
				String aWomanFirstName = matchesResultSet.getString("apf.first_name") ;
				String aWomanLastName  = matchesResultSet.getString("apf.last_name") ;
				String aManFirstName = matchesResultSet.getString("apm.first_name") ;
				String aManLastName  = matchesResultSet.getString("apm.last_name") ;
				Team aTeam = new Team(aWomanFirstName, aWomanLastName, aManFirstName, aManLastName) ; 
				String bWomanFirstName = matchesResultSet.getString("bpf.first_name") ;
				String bWomanLastName  = matchesResultSet.getString("bpf.last_name") ;
				String bManFirstName = matchesResultSet.getString("bpm.first_name") ;
				String bManLastName  = matchesResultSet.getString("bpm.last_name") ;
				Team bTeam = new Team(bWomanFirstName, bWomanLastName, bManFirstName, bManLastName) ;
				MatchResult matchResult = new MatchResult(league, date, aTeam, bTeam, teamAwins, teamBwins) ;
				allMatches.add(matchResult) ; 
			}
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}
		return allMatches ; 
	}

	public static League fetchLeague(int leagueId) {
		League league = null;
		Connection connection = fetchConnection();
		ResultSet leagueResultSet = null;
		String selectSql = "select day_of_week, year from dpva.league where id = "
				+ leagueId;
		DayOfTheWeek day_of_the_week = DayOfTheWeek.MONDAY;
		ArrayList<Team> allTeams = new ArrayList<Team>(10);
		int year = 0;
		try {
			Statement leagueStatement = connection.createStatement();
			leagueResultSet = leagueStatement.executeQuery(selectSql);
			while (leagueResultSet.next()) {
				String day = leagueResultSet.getString("day_of_week");
				if (DayOfTheWeek.THURSDAY.toString().equals(day)) {
					day_of_the_week = DayOfTheWeek.THURSDAY;
				}
				year = leagueResultSet.getInt("year");
			}
			// Now get all the teams;
			String teamsSql = " select t.id, pf.first_name, pf.last_name, pf.gender, pf.id, ";
			teamsSql += " pm.first_name, pm.last_name, pm.gender, pm.id ";
			teamsSql += " from team t ";
			teamsSql += " join person pf on pf.id = woman_id ";
			teamsSql += " join person pm on pm.id = man_id ";
			teamsSql += " where league_id = " + leagueId + " ;";
			Statement teamsStatement = connection.createStatement();
			ResultSet teamsResultSet = teamsStatement.executeQuery(teamsSql);
			while (teamsResultSet.next()) {
				String womanFirstName = teamsResultSet
						.getString("pf.first_name");
				String womanLastName = teamsResultSet.getString("pf.last_name");
				String womanGender = teamsResultSet.getString("pf.gender"); 
				Person woman = new Person(womanFirstName, womanLastName,
						womanGender);

				String manFirstName = teamsResultSet.getString("pm.first_name");
				String manLastName = teamsResultSet.getString("pm.last_name");
				String manGender = teamsResultSet.getString("pm.gender"); 
				Person man = new Person(manFirstName, manLastName, manGender);
				Team team = new Team(man, woman, year, day_of_the_week);
				allTeams.add(team);
			}
			league = new League(leagueId, allTeams, year, day_of_the_week);
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}

		return league;
	}

	public static ArrayList<Person> fetchAllPeopleInDatabase() {
		Connection connection = fetchConnection();
		ResultSet personResultSet = null;
		String selectSql = "select id, first_name, last_name, gender from dpva.person " ;
		ArrayList<Person> allPeople = new ArrayList<Person>(100);
		try {
			Statement personStatement = connection.createStatement();
			personResultSet = personStatement.executeQuery(selectSql);
			while (personResultSet.next()) {
				String firstName = personResultSet.getString("first_name");
				String lastName = personResultSet.getString("last_name");
				String gender = personResultSet.getString("gender");
				int id = personResultSet.getInt("id") ;
				Person person = new Person(id, firstName, lastName, gender);
				allPeople.add(person);
			}
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}
		return allPeople ;		
	}
	
	public static void main(String[] arguments) {
		System.out.println(htmlOfStandings(1));

		Connection connection = fetchConnection();
		String selectSql = "select pAf.first_name, pBf.first_name, mr.team_A_wins, mr.team_B_wins, mr.play_date, l.day_of_week, ";
		selectSql += "l.year from match_result mr ";
		selectSql += "join team tA on mr.team_A_id = tA.id ";
		selectSql += "join team tB on mr.team_B_id = tB.id ";
		selectSql += "join person pAf on tA.woman_id = pAf.id ";
		selectSql += "join person pBf on tB.woman_id = pBf.id ";
		selectSql += "join league l on mr.league_id = l.id ; ";
		try {
			Statement selectStatement = connection.createStatement();
			ResultSet selectResultSet = selectStatement.executeQuery(selectSql);
			while (selectResultSet.next()) {
				String matchResult = selectResultSet.getString(1);
				matchResult += " vs " + selectResultSet.getString(2);
				matchResult += ": " + selectResultSet.getInt(3) + "-"
						+ selectResultSet.getInt(4);

				System.out.println(matchResult);
			}
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}
	}

	public static ArrayList<Schedule> fetchAllScheduledMatches(int leagueId,
			League league) {
		ArrayList<Schedule> allSchedules = new ArrayList<Schedule>() ; 
		Connection connection = fetchConnection();
		try {
			// Now get all the teams;
			String selectSchedulesSql = " select ta.team_name, tb.team_name, s.play_date  ";
			selectSchedulesSql += " from dpva.schedule s ";
			selectSchedulesSql += " join team ta on ta.id = s.team_A_id ";
			selectSchedulesSql += " join team tb on tb.id = s.team_B_id ";
			selectSchedulesSql += " where s.league_id = " + leagueId + " ;";
			Statement schedulesStatement = connection.createStatement();
			ResultSet schedulesResultSet = schedulesStatement.executeQuery(selectSchedulesSql);
			while (schedulesResultSet.next()) {
				Date date = schedulesResultSet.getDate("s.play_date") ;
				String teamAName = schedulesResultSet.getString("ta.team_name") ;
				String teamBName = schedulesResultSet.getString("tb.team_name") ;
				Team aTeam = new Team(teamAName) ; 
				Team bTeam = new Team(teamBName) ;
				Schedule matchResult = new Schedule(aTeam, bTeam, league, 0, date) ;
				allSchedules.add(matchResult) ; 
			}
		} catch (SQLException sqlException) {
			String message = "\nFailed to fetch schedules. " ;
			message += "sqlException message: \n" + sqlException.getMessage() ;
			System.out.println(message) ;
			sqlException.printStackTrace();
		}
		return allSchedules ; 
	}

	/**
	 * insert into dpva.league (day_of_week, year) values ('Thursday', 2014) ;
	 * @param dayOfWeek
	 * @param year
	 * @param divisionCount
	 */
	public static void createNewLeague(String dayOfWeek, int year, int divisionCount) {
		String insertSql = "insert into dpva.league (day_of_week, year) values (" ;
		insertSql += "'" + dayOfWeek + "', " ;
		insertSql += year ;
//		insertSql += year + ", " ; // division not yet implemented.
		insertSql += ") " ;
		executeInsertStatement(insertSql) ; 
	}

	private static int executeInsertStatement(String insertSql) {
		Connection connection = fetchConnection();
		int resultingId = -1 ; 
		
		try {
			Statement statement = connection.createStatement() ;
			statement.execute(insertSql, Statement.RETURN_GENERATED_KEYS) ;
			ResultSet keyResultSet = statement.getGeneratedKeys() ;
			keyResultSet.next() ; 
			resultingId = keyResultSet.getInt(1) ; // not currently working 
		} catch (SQLException sqlException) {
			System.out.println(sqlException.getMessage()) ;
			sqlException.printStackTrace() ;
		} 
		return resultingId ; 
	}

	public static int insertPerson(Person person) {
		String insertSql = "insert into dpva.person(first_name, last_name, gender) values (" ;
		insertSql += "'" + person.getFirstName() + "', " ;
		insertSql += "'" + person.getLastName()  + "', " ;
		String genderString = "'F'" ; 
		if(person.isMale()) {
			genderString = "'M'" ; 
		}
		insertSql += genderString ;
		insertSql += ") " ;
		int id = executeInsertStatement(insertSql) ;
		return id ; 
	}

	public static int insertNewTeam(int league, Person man, Person woman, String teamName) {
		String insertSql = "insert into dpva.team(league_id, man_id, woman_id, team_name) values (" ;
		insertSql += league + ", " ; 
		insertSql += man.getId() + ", " ; 
		insertSql += woman.getId() + ", " ; 
		insertSql += "'" + teamName + "'" ;
		insertSql += ") " ;
		int id = executeInsertStatement(insertSql) ;
		return id ; 
	}

	public static ArrayList<League> fetchLeagueList() {
		ArrayList<League> allLeagues = new ArrayList<League>() ;
		Connection connection = fetchConnection();
		ResultSet leagueResultSet = null;
		String selectSql = "select id, day_of_week, year from dpva.league " ;
		try {
			Statement leagueStatement = connection.createStatement();
			leagueResultSet = leagueStatement.executeQuery(selectSql);
			while (leagueResultSet.next()) {
				DayOfTheWeek day_of_the_week = DayOfTheWeek.MONDAY ; 
				String day = leagueResultSet.getString("day_of_week");
				if (DayOfTheWeek.THURSDAY.toString().equals(day)) {
					day_of_the_week = DayOfTheWeek.THURSDAY;
				}
				
				int year = leagueResultSet.getInt("year");
				int id = leagueResultSet.getInt("id") ; 
				allLeagues.add( new League(id, year, day_of_the_week));
			}
		} catch (SQLException sqlException) {
			sqlException.printStackTrace();
		}

		return allLeagues ;
	}

	/**
	 * This method assumes that only the teamName in the team object is 
	 * correctly filled in. 
	 * @param matchesInRequest
	 */
	public static void saveMatches(ArrayList<MatchResult> matchesInRequest) {
		for (Iterator<MatchResult> matchIterator = matchesInRequest.iterator(); matchIterator
				.hasNext();) {
			MatchResult matchResult =  matchIterator.next();
			if(matchResult.containsWins()) {
				insertMatchResult(matchResult) ;
			}
		}
	}

	private static int insertMatchResult(MatchResult matchResult) {
		String insertSql = "insert into dpva.match_result(play_date, league_id, team_A_id, team_B_id, team_A_wins, team_B_wins) values (" ;
		insertSql += "current_date()" + ", " ; 
		insertSql += matchResult.getLeague().getId() + ", " ; 
		insertSql += matchResult.getTeamA().getTeamId() + ", " ; 
		insertSql += matchResult.getTeamB().getTeamId() + ", " ; 
		insertSql += matchResult.getTeamAwins() + ", " ; 
		insertSql += matchResult.getTeamBwins() ; 
		insertSql += ") " ;
		System.out.println("insertSql: " + insertSql) ; 
		int id = executeInsertStatement(insertSql) ;
		return id ; 
	}

	public static Team fetchTeam(String teamName, int leagueId) {
		Team team = null ; 
		Connection connection = fetchConnection();
		ResultSet teamResultSet = null;
		String selectSql = "select id, man_id, woman_id from dpva.team " ; 
		selectSql += " where league_id = " + leagueId ;
		selectSql += " and team_name = '" + teamName + "'" ; 
		// Maybe fetch and create the entire person - someday. 
		System.out.println("fetchTeam sql: " + selectSql) ;
		try {
			Statement fetchTeamStatement = connection.createStatement();
			teamResultSet = fetchTeamStatement.executeQuery(selectSql);
			teamResultSet.next() ; // Should only get one team.
			int teamId = teamResultSet.getInt("id") ;
			int manId = teamResultSet.getInt("man_id") ;
			int womanId = teamResultSet.getInt("woman_id") ;
			team = new Team(teamId, leagueId, manId, womanId, teamName) ; 
			System.out.println("team: " + team) ;
		}
		catch(SQLException sqlException) {
			System.out.println("Sql error in fetchTeam(). Message: " + sqlException.getMessage()) ;
		}
		return team ; 
	}
	
}
