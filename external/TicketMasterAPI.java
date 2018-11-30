package external;

import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import db.MyConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Item;
import entity.Item.ItemBuilder;
import db.*;

public class TicketMasterAPI {
    private static final String URL = "https://app.ticketmaster.com/discovery/v2/events.json";
    private static final String DEFAULT_KEYWORD = "";
    private static final String API_KEY = "IaBk7IByhaArrAiVa2rxGm613vd8LM3i";

    private static final String EMBEDDED = "_embedded";
    private static final String EVENTS = "events";
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String URL_STR = "url";
    private static final String RATING = "rating";
    private static final String DISTANCE = "distance";
    private static final String VENUES = "venues";
    private static final String ADDRESS = "address";
    private static final String LINE1 = "line1";
    private static final String LINE2 = "line2";
    private static final String LINE3 = "line3";
    private static final String CITY = "city";
    private static final String IMAGES = "images";
    private static final String CLASSIFICATIONS = "classifications";
    private static final String SEGMENT = "segment";



    public List<Item> search(double lat, double lon, String keyword) {
        //之前的返回值是jsonarray，现在要改成list
        //Normalize HTTP string
        if (keyword == null) {
            keyword = DEFAULT_KEYWORD;
        }
        //将不能出现在URL中的字符转化成UTF-8
        try {
            keyword = java.net.URLEncoder.encode(keyword, "UTF-8");
        } catch (Exception e){
            e.printStackTrace();
        }

        String geoHash = GeoHash.encodeGeohash(lat, lon, 8);

        //construct URL string
        String query = String.format("apikey=%s&geoPoint=%s&keyword=%s&radius=%s",
                API_KEY, geoHash, keyword, 50);


        try {
            //真的跟这个地址connect了一次;完成了对endpoint的一次访问

            //Initiate HTTP request
            HttpURLConnection connection = (HttpURLConnection) new URL(URL + "?" + query).openConnection();//先发送，再获取
            int responseCode = connection.getResponseCode();

            System.out.println("\nSending 'GET' request to URL : " + URL + "?" + query);
            System.out.println("Response Code: " + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;

            //Get response string
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject obj = new JSONObject(response.toString());
            if (obj.isNull(EMBEDDED)) {//此次查询没有结果
                return new ArrayList<>();//返回一个空的array
            }


            //Convert to JSON format and get the key we care baout
            JSONObject embedded = obj.getJSONObject(EMBEDDED);
            JSONArray events = embedded.getJSONArray(EVENTS);
            return getItemList(events);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    //用来degbug的函数：
    private void queryAPI(double lat, double lon) {
        List<Item> events = search(lat, lon, null);
        MyConnection conn = new MyConnection();
        for (Item item: events) {
            System.out.println(item.getItemId());
            System.out.println(item.getAddress());
            conn.saveItem(item);;
        }
        try {
            for (Item item : events) {
                JSONObject jsonObkect = item.toJSONObject();
                ////System.out.println(jsonObkect);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getAddress(JSONObject event) throws JSONException {
        //因为比较复杂，所以单独拿出来实现
        if (!event.isNull(EMBEDDED)) {
            //先看event当中有没有另一个embedded
            JSONObject embedded = event.getJSONObject(EMBEDDED);

            if (!embedded.isNull(VENUES)) {
                JSONArray venues = embedded.getJSONArray(VENUES);
                for (int i = 0; i < venues.length(); i++) {
                    //写成循环的好处是，虽然现在版本的API是每次都能得到结果，但如果这个API改了，以后返回的万一是空就gg了。写成循环就能确保能找到不为空的
                    JSONObject venue = venues.getJSONObject(i);

                    StringBuilder sb = new StringBuilder();

                    if (!venue.isNull(ADDRESS)) {
                        JSONObject address = venue.getJSONObject(ADDRESS);

                        if (!address.isNull(LINE1)) {
                            sb.append(address.getString(LINE1));
                        }
                        if (!address.isNull(LINE2)) {
                            sb.append(' ');
                            sb.append(address.getString(LINE2));
                        }
                        if (!address.isNull(LINE3)) {
                            sb.append(' ');
                            sb.append(address.getString(LINE3));
                        }
                    }
                    if (!venue.isNull(CITY)) {
                        JSONObject city = venue.getJSONObject(CITY);
                        if (!city.isNull(NAME)) {
                            sb.append('\n');
                            sb.append('\n');
                            sb.append(city.getString(NAME));
                        }
                    }
                    String addr = sb.toString();
//					if (!addr.equals("")) {
                    return addr;
                    //}
                }
            }
        }
        return "";
    }


    // {"images": [{"url": "www.example.com/my_image.jpg"}, ...]}
    private String getImageUrl(JSONObject event) throws JSONException {
        if (!event.isNull(IMAGES)) {
            JSONArray images = event.getJSONArray(IMAGES);
            for (int i = 0; i < images.length(); i++) {
                JSONObject image = images.getJSONObject(i);
                if (!image.isNull(URL_STR)) {
                    String urlStr = image.getString(URL_STR);
                    return urlStr;
                }
            }
        }
        return "";
    }

    // {"classifications" : [{"segment": {"name": "music"}}, ...]}
    private Set<String> getCategories(JSONObject event) throws JSONException {
        Set<String> categories = new HashSet<>();
        //这次不是拿到一个就返回了，而是要把循环跑完都加完才行
        if (!event.isNull(CLASSIFICATIONS)) {
            JSONArray classifications = event.getJSONArray(CLASSIFICATIONS);
            for (int i = 0; i < classifications.length(); i++) {
                JSONObject classification = classifications.getJSONObject(i);
                if (!classification.isNull(SEGMENT)) {
                    JSONObject segment = classification.getJSONObject(SEGMENT);
                    if (!segment.isNull(NAME)) {
                        categories.add(segment.getString(NAME));
                    }
                }
            }
        }

        return categories;
    }

    // Convert JSONArray to a list of item objects.
    private List<Item> getItemList(JSONArray events) throws JSONException {
        List<Item> itemList = new ArrayList<>();

        for (int i = 0; i < events.length();i++) {
            JSONObject event = events.getJSONObject(i);
            ItemBuilder builder = new ItemBuilder();
            if (!event.isNull(NAME)) {
                // 是null的话为什么不用抛异常？因为我们用了builder pattern，即每次不一定能获得所有object的参数。如果没有的话就用默认string“”
                builder.setName(event.getString(NAME));
            }
            if (!event.isNull(ID)) {
                builder.setItemId(event.getString(ID));
            }
            if (!event.isNull(URL_STR)) {
                builder.setUrl(event.getString(URL_STR));
            }
            if (!event.isNull(RATING)) {
                builder.setRating(event.getDouble(RATING));
            }
            if (!event.isNull(DISTANCE)) {
                builder.setDistance(event.getDouble(DISTANCE));
            }

            //简单的都写完了，剩下几个要写helper function来实现
            builder.setAddress(getAddress(event));
            builder.setCategories(getCategories(event));
            builder.setImageUrl(getImageUrl(event));

            itemList.add(builder.build());
        }
        return itemList;
    }

    public static void main(String[] args) {
        TicketMasterAPI tmApi = new TicketMasterAPI();
        // Mountain View, CA
        // tmApi.queryAPI(37.38, -122.08);
        // London, UK
        // tmApi.queryAPI(51.503364, -0.12);
        // Houston, TX
        ///tmApi.queryAPI(29.682684, -95.295410);
        tmApi.queryAPI(37.38, -122.08);


    }
}


