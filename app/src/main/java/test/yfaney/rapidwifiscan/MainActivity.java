package test.yfaney.rapidwifiscan;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends ActionBarActivity {

    boolean isReadyFile = false;
    String mPackageName = null;
    byte[] buffer = new byte[1024];

    EditText editTextResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editTextResult = (EditText) findViewById(R.id.editTextResult);
        try{
            PackageManager m = getPackageManager();
            String s = getPackageName();
            PackageInfo p = m.getPackageInfo(s, 0);
            mPackageName = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("yourtag", "Error Package name not found. Please check your system", e);
            finish();
        }

    }

    @Override
    protected void onStart(){
        super.onStart();
        new AsyncTask<Void, Process, Integer>(){

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    Process su = Runtime.getRuntime().exec("su -c pwd");
                    InputStream is = su.getInputStream();
                    int read;
                    while((read= is.read(buffer))>0){
                        Log.d("Proecss", new String(buffer));
                    }
                    return su.waitFor();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return -1;
            }

            @Override
            public void onPostExecute(Integer result){
                Log.d("Process", "SU returns code :" + Integer.toString(result));
                if(result == 0){
                    Toast.makeText(getBaseContext(), "Root Permission Allowed.", Toast.LENGTH_SHORT).show();
                    readyFile();
                }else{
                    Toast.makeText(getBaseContext(), "This app needs a root permission.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }.execute();
    }

    public void onReadyClicked(View v){
        readyFile();
    }

    protected void readyFile(){
        super.onResume();
        if(isReadyFile) return;

        new AsyncTask<Void, Process, Integer>(){

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    AssetManager assetManager = getBaseContext().getAssets();
                    String[] files = null;
                    files = assetManager.list("files");
                    for(String filename : files) {
                        InputStream in = null;
                        OutputStream out = null;
                        try {
                            in = assetManager.open("files/" + filename);
                            File outFile = new File(mPackageName, filename);
                            if(outFile.isDirectory()) continue;
                            out = new FileOutputStream(outFile);
                            copyFile(in, out);
                            in.close();
                            in = null;
                            out.flush();
                            out.close();
                            out = null;
                        } catch(IOException e) {
                            Log.e("tag", "Failed to copy asset file: " + filename, e);
                        }
                    }
                    String[] cmd = {"su", "-c", "chmod 755 " + mPackageName + "/" + "iwlist"};
                    printCmdLine(cmd);
                    Process su = Runtime.getRuntime().exec(cmd);
                    InputStream is = su.getInputStream();
                    int read;
                    while((read= is.read(buffer))>0){
                        Log.d("Process", new String(buffer));
                    }
                    int exitCode = su.waitFor();
                    return exitCode;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return -1;
            }

            @Override
            public void onPostExecute(Integer result){
                Log.d("Process", "Returns: " + Integer.toString(result));
                if(result ==0){
                    isReadyFile = true;
                }else if (result == 127) {
                    Toast.makeText(getBaseContext(), "SU returns Command Not Found: " + Integer.toString(result), Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getBaseContext(), "Oh, something wrong: " + Integer.toString(result), Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
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
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onScanClicked(View view){
        editTextResult.setText("");
        if(!isReadyFile){
            Toast.makeText(this, "Please click ReadyFile first.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AsyncTask<Void, Process, String>(){

            @Override
            protected String doInBackground(Void... params) {
                try {
                    String[] cmd = {"su", "-c", mPackageName + "/" + "iwlist wlan0 scanning | sed -n '/Address:/p/Channel:/p/ESSID/p/Last beacon/p'"};
                    printCmdLine(cmd);
                    Process su = Runtime.getRuntime().exec(cmd);
                    InputStream is = su.getInputStream();
                    int read;
                    StringBuffer lBuffer = new StringBuffer();
                    while((read= is.read(buffer))>0){
                        String bf = new String(buffer);
                        Log.d("Proecss", bf);
                        lBuffer.append(bf);
                    }
                    int waitcode =  su.waitFor();
                    Log.d("ReturnCode", Integer.toString(waitcode));
                    return lBuffer.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void onPostExecute(String result){
                if(result != null){
                    Toast.makeText(getBaseContext(), "Root Permission Allowed.", Toast.LENGTH_SHORT).show();
                    editTextResult.setText(result);
                }else{
                    Toast.makeText(getBaseContext(), "This app needs a root permission.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute();
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
    private static void printCmdLine(String[] cmds){
        String cmdLine = "";
        for(String c : cmds){
            cmdLine = cmdLine + " " + c;
        }
        Log.d("Running", cmdLine);
    }
}
