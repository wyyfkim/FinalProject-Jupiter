package db;

public class DBConnectionUitl {
    private static final String HOSTNAME = "localhost";
    private static final String PORT_NUM = "8889"; //change it to your mysql port number
    //之前是8889是MAMP自带的，现在要改成3306，which is mysql默认端口
    public static final String DB_NAME = "laiproject";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "root";
    public static final String URL = "jdbc:mysql://"
            + HOSTNAME + ":" + PORT_NUM + "/" + DB_NAME
            + "?user=" + USERNAME + "&password=" + PASSWORD
            + "&autoReconnect=true&serverTimezone=UTC";
    // jdbc:mysqul://localhost:8889/laiproject?user=rot&password=root&autoReconnect=true&serverTimezone=UTC
//JDBC给developer提供API，JDBC与DB的交互是由DB提供的driver来实现的，即reflect
}