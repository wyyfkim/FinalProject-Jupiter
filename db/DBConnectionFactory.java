package db;

import db.MyConnection;

public class DBConnectionFactory {
    // This should change based on the pipeline.
    private static final String DEFAULT_DB = "mysql";

    public static DBConnection getConnection(String db) {
        //为啥是静态？鸡蛋问题，跟之前的itemBuilder原理一样
        //工厂方法创建好处：所有人遵循一个方法来创建connection。你的客户不需要知道你用的啥connection。
        switch (db) {
            case "mysql":
                return new MyConnection();
            case "mongodb":
                // return new MongoDBConnection();
                return null;
            default:
                throw new IllegalArgumentException("Invalid db: " + db);
        }
    }

    public static DBConnection getConnection() {
        return getConnection(DEFAULT_DB);
    }

}
