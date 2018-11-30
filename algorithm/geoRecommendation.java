package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;

public class geoRecommendation {
    public List<Item> recommendItems(String userId, double lat, double lon) {

        List<Item> recommendedItems = new ArrayList<>();
        DBConnection conn = DBConnectionFactory.getConnection();

        try {
            //step1: Get all favorite items
            //要先get itemId，因为在category表里面是靠itemId来查询category的，而且我们也不需要item里的其它信息
            Set<String> favoriteItemIds = conn.getFavoriteItemIds(userId);

            //step2: Get all categories of favorite items, sort by count
            Map<String, Integer> allCategories = new HashMap<>();
            for (String itemId : favoriteItemIds) {
                Set<String> categories = conn.getCategories(itemId);
                for (String category : categories) {
                    allCategories.put(category, allCategories.getOrDefault(category, 0) + 1);
                }
            }
            List<Entry<String, Integer>> categoryList = new ArrayList<>(allCategories.entrySet());
            Collections.sort(categoryList, (o1, o2) -> Integer.compare(allCategories.get(o2), allCategories.get(o1)));

            //step3: do search based on category, filer out favorited events, sort by distance
            //visitedItem其实是search结果中已经出现过了的,但是现在还不能正常工作因为是new item没有比较函数，需要equal和hashcode（在item类里面添加）

            Set<Item> visitedItems = new HashSet<>();
            for (Entry<String, Integer> category : categoryList) {
                //现在以category作为keyWord来搜索
                List<Item> items = conn.searchItems(lat, lon, category.getKey());
                List<Item> filteredItems = new ArrayList<>();
                for (Item item : items) {
                    if (!favoriteItemIds.contains(item.getItemId()) && !visitedItems.contains(item)) {
                        filteredItems.add(item);
                    }
                }
                //按照distance排序
                Collections.sort(filteredItems, (o1, o2) -> Double.compare(o1.getDistance(), o2.getDistance()));

                visitedItems.addAll(items);
                recommendedItems.addAll(filteredItems);

            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return recommendedItems;
    }
}
