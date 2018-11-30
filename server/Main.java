package server;

import algorithm.geoRecommendation;
import db.DBConnection;
import db.DBConnectionFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

import static spark.Spark.*;


public class Main {
    private static final long serialVersionUID = 1L;
    public static void main(String[] args) {
        port(1236);
        //search
        get("/search", (req, res) -> {
            double lat = Double.parseDouble(req.queryParamsValues("lat")[0]);
            double lon = Double.parseDouble(req.queryParamsValues("lon")[0]);
//            String term = req.queryParamsValues("term")[0];
            System.out.println(lat + ": " + lon);
            //这里要reflect user已经fav过的item
            String userId = req.queryParamsValues("user_id")[0];
            //没联入数据库的版本：
		    //TicketMasetAPI tmAPI = new TicketMasetAPI();
    		//List<Item> items = tmAPI.search(lat, lon, keyword);
            DBConnection connection = null;
            try {
                connection = DBConnectionFactory.getConnection();
            } catch (Exception e) {
                System.out.println("database failed");
            }
            try {
                List<entity.Item> items = connection.searchItems(lat, lon, null);
                Set<String> favoriteItems = connection.getFavoriteItemIds(userId);
                JSONArray array = new JSONArray();
                for (entity.Item item : items) {
                    JSONObject obj = item.toJSONObject();
                    obj.put("favorite", favoriteItems.contains(item.getItemId()));
                    array.put(obj);
                }
                return array;
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            } finally {
                connection.close();
            }
        });

        //favorite
        get("/history", (req, res) -> {
            //查询某个人favorite的历史，需要userId，这个也是param
            String userId = req.queryParamsValues("user_id")[0];
            JSONArray array = new JSONArray();
            DBConnection conn = DBConnectionFactory.getConnection();
            try {
                Set<entity.Item> items = conn.getFavoriteItems(userId);
                for (entity.Item item : items) {
                    JSONObject obj = item.toJSONObject();
                    obj.append("favorite", true);
                    //这一步在前段判断也可以，但在这里写更加方便。因为业务逻辑尽量在后端实现
                    array.put(obj);
                }
                return items;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            } finally {
                conn.close();
            }
        });
        post("/history", (req, res) -> {
            DBConnection conn = DBConnectionFactory.getConnection();
            try {
//                JSONObject input = RpcHelper.readJsonObject(request);
                String userId = req.queryParamsValues("user_id")[0];
                JSONArray array = new JSONArray(req.queryParamsValues("favorite"));
                List<String> itemIds = new ArrayList<>();
                //遍历array把JsonArray里面的内容读出来
                for (int i = 0; i < array.length(); ++i) {
                    itemIds.add(array.getString(i));
                }
                conn.setFavoriteItems(userId, itemIds);
                return "Success";
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed!";
            } finally {
                conn.close();
            }
        });
        delete("/history", (req, res) -> {
            DBConnection conn = DBConnectionFactory.getConnection();
            try {
                String userId = req.queryParamsValues("user_id")[0];
                JSONArray array = new JSONArray(req.queryParamsValues("favorite"));

                List<String> itemIds = new ArrayList<>();
                //遍历array把JsonArray里面的内容读出来
                for (int i = 0; i < array.length(); ++i) {
                    itemIds.add(array.getString(i));
                }
                conn.unsetFavoriteItems(userId, itemIds);
                return "Success";
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed!";
            } finally {
                conn.close();
            }
        });

        //recommendation
        get("/recommendation", (req, res) -> {
            JSONArray array = new JSONArray();
            String userId = req.queryParamsValues("user_id")[0];
            double lat = Double.parseDouble(req.queryParamsValues("lat")[0]);
            double lon = Double.parseDouble(req.queryParamsValues("lon")[0]);
            geoRecommendation recommendation = new geoRecommendation();
            List<entity.Item> items = recommendation.recommendItems(userId, lat, lon);
            try {
                for (entity.Item item : items) {
                    array.put(item.toJSONObject());
                }
                return array;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}
