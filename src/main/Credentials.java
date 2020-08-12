package main;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Credentials {

    private String username = "";
    private String secret = "";

    public Credentials(String path){
        try{
            ArrayList<String> secrets = new ArrayList(Files.readAllLines(Paths.get(path)));

            for(String s : secrets){
                String[] tmp = s.split("=");
                if(tmp.length < 2) continue;
                if(tmp[0].equals("username")){
                    username = tmp[1];
                } else if(tmp[0].equals("secret")){
                    secret = tmp[1];
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getUsername(){
        return username;
    }

    public String getSecret(){
        return secret;
    }
}
