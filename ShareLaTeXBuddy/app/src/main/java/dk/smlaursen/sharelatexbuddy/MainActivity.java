package dk.smlaursen.sharelatexbuddy;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.Map;

import javax.security.auth.login.LoginException;


public class MainActivity extends ActionBarActivity {

    private ListView mProjectsList;
    private SwipeRefreshLayout mSwipeLayout;
    private Map<String, String> mProjectsMap;

    private ArrayList<String> mProjectsListFile = new ArrayList<String>();
    private ArrayAdapter myArrayAdapter;

    private ShareLaTeXAuthenticator mAuthenticator;

    private boolean isBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Layout
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#a93529")));
        bar.setTitle("ShareLaTeX Buddy");
        setContentView(R.layout.activity_main);

        //ListView
        myArrayAdapter  = new ArrayAdapter<String>(MainActivity.this, R.layout.custom_list_item, R.id.list_content, mProjectsListFile);
        mProjectsList = (ListView) findViewById(R.id.listview);
        mProjectsList.setAdapter(myArrayAdapter);
        mProjectsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("OnItemClick", "User selected on item " + mProjectsListFile.get(position) + (mSwipeLayout.isRefreshing() ? "Ignored, busy with other task" : ""));
                //Ignore subsequent clicks while loading
                if(!isBusy){
                    //Convert to projectID
                    String projectID = mProjectsMap.get(mProjectsListFile.get(position));
                    new displayPDFTask().execute(projectID);
                }
            }
        });

        //Refresh on swipe
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("setOnRefreshListener", "User refreshing");
                new GetProjectsTask().execute();
            }
        });
        mAuthenticator = new ShareLaTeXAuthenticator(getContext());
        mAuthenticator.useCredentials("test@test.dk", "testtest");
        mAuthenticator.storeCredentials();
        mAuthenticator.loadCredentials();
        Authenticator.setDefault(mAuthenticator);
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
    }

    @Override
    protected void onStart(){
        super.onStart();

        //If not already refreshing
        if(!isBusy){
            Log.d("OnStart called","Refreshing project list");
            new GetProjectsTask().execute();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_logout:
                Log.d("LogOut","Performing logout");
                //Clear last used credentials :
                mAuthenticator.clearCredentials();
                mProjectsListFile.clear();
                mProjectsMap.clear();
                myArrayAdapter.notifyDataSetChanged();

                //TODO Go to login screen here

                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    private Context getContext(){
        return this;
    }

    /** Helper method for displaying Errors, and routing to LoginScreen if LoginException*/
    private void presentReasonForFailure(String failure, final Exception exception){
        AlertDialog ad = new AlertDialog.Builder(getContext()).create();
        ad.setCancelable(false); // This blocks the 'BACK' button
        ad.setMessage(failure+" :\n"+exception.getMessage());
        ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //Go to Login Screen and try to login
                if (exception instanceof LoginException) {
                    //TODO Go To Login Screen Here
                }
            }
        });
        ad.setIcon(android.R.drawable.ic_dialog_alert);
        ad.show();
    }

    /** This task retrieves all projects visible to the user and displays them on the listview*/
    private class GetProjectsTask extends AsyncTask<Void, Void, Exception> {
        @Override
        protected void onPreExecute() {
            isBusy = true;
            mSwipeLayout.setRefreshing(true);
        }
        @Override
        protected Exception doInBackground(Void...arg0){
            //Retrieve the projects and return the exception if not possible
            try {
                mProjectsMap = ShareLaTeXFacade.getMapOfProjects();
                return null;
            } catch (Exception e){
                Log.d(this.getClass().getSimpleName(),"Got Exception "+e.getClass()+" "+e.getMessage());
                return e;
            }
        }
        @Override
        protected void onPostExecute(final Exception exception){
            //If succeded update table
            mSwipeLayout.setRefreshing(false);
            if(exception == null){
                Log.d("GetProjectTask","Retrieved "+mProjectsMap.keySet());
                mProjectsListFile.clear();
                mProjectsListFile.addAll(mProjectsMap.keySet());
                myArrayAdapter.notifyDataSetChanged();
            }
            //Present reason to user
            else {
                Log.d("GetProjecTask","Showing alertdialog with message "+exception.getMessage());
                presentReasonForFailure("Unable to retrieve projects", exception);
            }
            isBusy = false;
        }
    }
    /** This task takes a specified project and compile,download and displays it*/
    private class displayPDFTask extends AsyncTask<String, Void, Exception> {
        private File pdfFile;

        @Override
        protected void onPreExecute() {
            isBusy = true;
            mSwipeLayout.setRefreshing(true);
        }
        @Override
        protected Exception doInBackground(String...arg0){
            String id = arg0[0].toString();
            //Retrieve the projects and return the exception if not possible
            try {
                Log.d(this.getClass().getSimpleName(),"compiling "+id);
                ShareLaTeXFacade.compilePDF(id);

                Log.d(this.getClass().getSimpleName(),"retrieving compiled "+id);
                pdfFile = ShareLaTeXFacade.retrievePDF(id);

                //return no exceptions
                return null;
            } catch (Exception e){
                Log.d(this.getClass().getSimpleName(),"Got Exception "+e.getClass()+" "+e.getMessage());
                return e;
            }
        }
        @Override
        protected void onPostExecute(final Exception exception){
            //If succeded display pdfFile
            mSwipeLayout.setRefreshing(false);
            if(exception == null){
                try{
                    Log.d(this.getClass().getSimpleName(), "Displaying File " + pdfFile.getName());
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(pdfFile), "application/pdf");
                    startActivity(intent);
                } catch (ActivityNotFoundException e){
                    presentReasonForFailure("Could not display downloaded pdf", e);
                }
            }
            //Present reason to user
            else {
                presentReasonForFailure("Could not display pdf", exception);
            }
            isBusy = false;
        }
    }
}
