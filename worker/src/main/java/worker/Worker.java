package worker;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.sql.*;
import org.json.JSONObject;

class Worker {
  public static void main(String[] args) {
    try {
      Jedis redis = connectToRedis("redis");
      Connection dbConn = connectToDB("db");

      System.err.println("Watching vote queue");

      while (true) {
        String voteJSON = redis.blpop(0, "votes").get(1);
        JSONObject voteData = new JSONObject(voteJSON);
        String voterID = voteData.getString("voter_id");
        String vote = voteData.getString("vote");
        String questionid = voteData.getString("questionid");

        System.err.printf("Processing vote for '%s' by '%s' for question '%s'\n", vote, voterID,questionid);
        System.err.printf("Processing vote Hello World \n");

        updateVote(dbConn, voterID, vote, questionid);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  static void updateVote(Connection dbConn, String voterID, String vote, String questionid) throws SQLException {

    PreparedStatement insert = dbConn.prepareStatement(
      "INSERT INTO votes (id, vote1, questionid) VALUES (?, ?, ?)");
    insert.setString(1, voterID);
    insert.setString(2, vote);
    insert.setString(3, questionid);

    try {
      insert.executeUpdate();
      System.err.println("insertion successful!!!");

    } catch (SQLException e) {
      System.err.printf("You cannot change your vote \n");
    }
  }

  static Jedis connectToRedis(String host) {
    Jedis conn = new Jedis(host);

    while (true) {
      try {
        conn.keys("*");
        break;
      } catch (JedisConnectionException e) {
        System.err.println("Waiting for redis");
        sleep(1000);
      }
    }
    System.err.println("hello world to redis1");
    System.err.println("Connected to redis");
    return conn;
  }

  static Connection connectToDB(String host) throws SQLException {
    Connection conn = null;

    try {

      Class.forName("org.postgresql.Driver");
      String url = "jdbc:postgresql://" + host + "/postgres";

      while (conn == null) {
        try {
          conn = DriverManager.getConnection(url, "postgres", "postgres");
        } catch (SQLException e) {
          System.err.println("Waiting for db");
          sleep(1000);
        }
      }

      PreparedStatement st = conn.prepareStatement(
        "CREATE TABLE IF NOT EXISTS votes (id VARCHAR(255) NOT NULL UNIQUE, vote1 VARCHAR(255) NOT NULL, questionid VARCHAR(5) NOT NULL)");
      st.executeUpdate();

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.err.println("Hello World 1");
    System.err.println("----Connected to db by creating new postgres db------");
    return conn;
  }

  static void sleep(long duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      System.exit(1);
    }
  }
}
