package movements;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Movement {


    public double getChange(){
        return 0.0;
    }

    public double getTimestamp(){
        return 0.0;
    }

    public String getUser(){return "UNKNOWN";}



    public static HashMap<String, String> getUserMappings(String path){
        HashMap<String, String> results = new HashMap<>();
        try{
            ArrayList<String> text = new ArrayList(Files.readAllLines(Paths.get(path)));

            for(String s : text){
                String[] tmp = s.split("=");
                if(tmp.length < 2) continue;
                results.put(tmp[1], tmp[0]);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        for(Map.Entry<String, String> entry : results.entrySet()){
            if(!Moment.users.contains(entry.getValue())) Moment.users.add(entry.getValue());
        }

        return results;
    }


    public static String getUser(String transID, HashMap<String, String> userMapping){
        String result = userMapping.get(transID);
        if(result == null) result = "UNKNOWN";
        return result;
    }
}
