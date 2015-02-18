package dk.smlaursen.sharelatexbuddy;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.security.auth.login.LoginException;

/**
 * Created by Sune on 18-02-2015.
 */
public class ShareLaTeXFacade {
    private static final int TIMEOUT = 3000;
    private static final String baseUrl = "http://192.168.1.76:3000";

    private static ShareLaTeXFacade instance;

    //Constructor
    private ShareLaTeXFacade(){}

    public static synchronized ShareLaTeXFacade getInstance(){
        if(instance == null){
            instance = new ShareLaTeXFacade();
        }
        return instance;
    }

    //// PRIVATE HELPER METHODS ////
    private void validateResponseCode(int responseCode, String function) throws LoginException, UnsupportedOperationException{
        int responseCategory = responseCode / 100;
        String msg;
        switch (responseCategory){
            case 1:
            case 2:
            case 3: Log.d("validateResponseCode", "validated OK");
                return;
            case 4: msg = responseCode+" during "+function+ " : User Not Authenticated";
                Log.e("validateResponseCode",msg);
                throw new LoginException(msg);
            case 5: msg = responseCode+" during "+function+ " : ShareLaTeX-server unable to perform request";
                Log.e("validateResponseCode",msg);
                throw new UnsupportedOperationException(msg);
        }
    }

    //// SHARELATEX API METHODS ////
    public HashMap<String, String> getMapOfProjects() throws IOException,LoginException,UnsupportedOperationException{
        String url = baseUrl+"/api/project";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);

        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        validateResponseCode(responseCode, "getMapOfProjects");

        //If validated ok, read response
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();
        con.disconnect();

        //A map for holding the mappings between name and id of the projects
        HashMap<String, String> projectsMap = new HashMap<String,String>();

        //Parse each project name/id pair
        try{
            //Parse JSON into projects
            JSONObject jObject  = new JSONObject(response.toString());
            JSONArray jArray = jObject.getJSONArray("projects");
            for(int i=0;i < jArray.length(); i++){
                JSONObject oneObject = jArray.getJSONObject(i);
                //Skip deleted projects
                if(oneObject.has("archived") && oneObject.getString("archived").equals("true")){
                    continue;
                }
                String name = oneObject.getString("name");
                String id = oneObject.getString("_id");
                projectsMap.put(name, id);
            }
        //IF a JSON exception is catched, we received HTML instead of JSON indicating were not properly authenticated.
        } catch (JSONException e){
            Log.e("getMapOfProjects","Received JSONException "+e.getMessage()+" while parsing response : "+response.toString());
            throw new LoginException();
        }
        return projectsMap;
    }
}