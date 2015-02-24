package dk.smlaursen.sharelatexbuddy;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Created by Sune on 18-02-2015.
 */
public class ShareLaTeXAuthenticatorFactory {
    private static final String PREF_NAME = "ShareLaTeXPref";
    private static final String PREF_USERNAME = "email";
    private static final String PREF_PASSWORD = "password";

    private static Context mContext;
    private static String mUser, mPass;


    private ShareLaTeXAuthenticatorFactory(){

    }

    public static synchronized void setContext(Context c){
        mContext = c;
    }

    public static synchronized void useCredentials(String user, String pass){
        mUser = user;
        mPass = pass;
    }

    public static synchronized void clearCredentials() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_USERNAME);
        editor.remove(PREF_PASSWORD);
        editor.apply();
        mUser = "";
        mPass = "";
    }

    public static synchronized void storeCredentials() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_USERNAME,mUser);
        editor.putString(PREF_PASSWORD,mPass);
        editor.apply();
    }

    //The loadCredentials are called explicitly here instead of just being part of the the getPasswordAuthentication
    //Routine as that may be called many times and the sharedPreferences can be expensive to retrieve
    public static synchronized void loadCredentials(){
        SharedPreferences prefs = mContext.getSharedPreferences(PREF_NAME, mContext.MODE_PRIVATE);
        mUser = prefs.getString(PREF_USERNAME,"");
        mPass = prefs.getString(PREF_PASSWORD,"");
    }

    public static synchronized ShareLaTeXAuthenticator getAuthenticator(){
        return new ShareLaTeXAuthenticator();
    }

    private static class ShareLaTeXAuthenticator extends Authenticator{
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication(mUser, mPass.toCharArray()));
        }
    }
}

