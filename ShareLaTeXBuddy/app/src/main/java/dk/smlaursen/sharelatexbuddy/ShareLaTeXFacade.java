package dk.smlaursen.sharelatexbuddy;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

    //// PRIVATE HELPER METHODS ////

    /**This method validates the responsecode from a http-request and throws suitable errors if any*/
    private static void validateResponseCode(int responseCode, String function) throws LoginException, UnsupportedOperationException{
        int responseCategory = responseCode / 100;
        String msg;
        switch (responseCategory){
            case 1:
            case 2:
            case 3: Log.d("validateResponseCode", "validated OK");
                return;
            case 4: msg = responseCode+" during "+function+ " i.e. User Not Authenticated";
                Log.e("validateResponseCode",msg);
                throw new LoginException(msg);
            case 5: msg = responseCode+" during "+function+ " i.e. ShareLaTeX-server unable to perform request";
                Log.e("validateResponseCode",msg);
                throw new UnsupportedOperationException(msg);
        }
    }

    //// SHARELATEX API METHODS ////

    /** This method retrieves the projects visible to the logged in user which has to be stored using the CookieManager
     * @throws java.io.IOException When ShareLaTeX is unreachable
     * @throws javax.security.auth.login.LoginException if the user is not logged in
     * @throws java.lang.UnsupportedOperationException if ShareLaTeX could not execute our request for some reason.
     * @return HashMap<String,String> mapping between project names and ids*/
    public static HashMap<String, String> getMapOfProjects() throws IOException,LoginException,UnsupportedOperationException{
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

    /**This method instructs ShareLaTeX to compile the project with the given id.
     * @throws java.io.IOException When ShareLaTeX is unreachable
     * @throws javax.security.auth.login.LoginException if the user is not logged in properly
     * @throws java.lang.UnsupportedOperationException if ShareLaTeX could not execute our request for some reason.*/
    public static void compilePDF(String id) throws IOException, LoginException, UnsupportedOperationException{
        String url = baseUrl+"/api/project/"+id+"/compile";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);

        //Validate response-code
        int responseCode = con.getResponseCode();
        validateResponseCode(responseCode, "compilePDF");

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

        try{
            JSONObject jObject = new JSONObject(response.toString());
            //Check that it could compile correctly and return here if that is the case
            if(jObject.has("status") && jObject.get("status").equals("success")) {
                return;
            }
        } catch(JSONException e){
            Log.e("compilePDF","Unexpectedly received JSONException "+e);
        }
        //If did not compile properly throw exception
        throw new UnsupportedOperationException("ShareLaTeX was unable to compile project. /n The project may contain fatal syntax-errors. ");
    }
    /**This method fetches the pdf of the project with the given id.
     * @throws java.io.IOException When ShareLaTeX is unreachable
     * @throws javax.security.auth.login.LoginException if the user is not logged in properly
     * @throws java.lang.UnsupportedOperationException if ShareLaTeX could not execute our request for some reason.
     * @return the retrieved PDF-File*/
    public static File retrievePDF(String id) throws IOException, LoginException, UnsupportedOperationException{
        String url = baseUrl+"/api/project/"+id+"/output/output.pdf";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(TIMEOUT);
        con.setReadTimeout(TIMEOUT);

        //Validate response-code
        int responseCode = con.getResponseCode();
        validateResponseCode(responseCode, "retrievePDF");

        //If validated ok, create output file and start writing its contents
        File file = new File(Environment.getExternalStorageDirectory()+"/out.pdf");
        OutputStream outStream = new FileOutputStream(file);

        int read = 0;
        byte[] bytes = new byte[1024];
        InputStream in = con.getInputStream();
        while ((read = in.read(bytes)) != -1) {
            outStream.write(bytes, 0, read);
        }
        outStream.flush();
        outStream.close();
        con.disconnect();

        return file;
    }
}