package util;

import android.net.SSLCertificateSocketFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.fragment.app.Fragment;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import me.facuarmo.droidgate.R;

public abstract class ActivityNetworkWorker extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "ActivityNetworkWorker";
    private WeakReference<Fragment> targetFragment;
    private String request;
    private String encodedData;
    private String cookies;

    protected ActivityNetworkWorker() {
        this.targetFragment = null;
    }

    protected void setTargetFragment(Fragment fragment) {
        targetFragment = new WeakReference<>(fragment);
    }

    protected abstract void handleOutput(String serverOutput);

    protected abstract void showInternalError(final String message, final Fragment reference);

    protected void setRequest(String request) {
        this.request = request;
    }

    protected void setEncodedData(String encodedData) {
        this.encodedData = encodedData;
    }

    public String getCookies() {
        return cookies;
    }

    private void setCookies(String cookies) {
        this.cookies = cookies;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        final Fragment reference = targetFragment.get();

        if (reference == null) {
            return null;
        }

        String preURL = reference.getString(R.string.server_protocol) + reference.getString(R.string.server_hostname) + ":" + reference.getResources().getInteger(R.integer.server_port) + reference.getString(R.string.server_path);
        Log.d(TAG, "populateView: generated URL from resources: \"" + preURL + "\"");

        try {
            URL url = new URL(preURL);
            HttpsURLConnection httpsURLConnection = null;

            try {
                // Try to open a connection.
                httpsURLConnection = (HttpsURLConnection) url.openConnection();
                httpsURLConnection.setConnectTimeout(reference.getResources().getInteger(R.integer.connection_timeout));
                httpsURLConnection.setRequestMethod(reference.getString(R.string.server_request_method));

                // Set the cookies.
                String cookie = PreferenceManager.getDefaultSharedPreferences(reference.getContext()).getString(reference.getString(R.string.internal_key_session_id), null);
                Log.d(TAG, "doInBackground: cookie: " + cookie);
                httpsURLConnection.setRequestProperty(reference.getString(R.string.server_cookie_request_key), cookie);

                // TODO: DEBUGGING!!! REMOVE THIS FOR PRODUCTION.
                httpsURLConnection.setSSLSocketFactory(SSLCertificateSocketFactory.getInsecure(0, null));
                httpsURLConnection.setHostnameVerifier(new AllowAllHostnameVerifier());

                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter(reference.getString(R.string.server_request_param), request);

                StringBuilder query = new StringBuilder();

                query.append(builder.build().getEncodedQuery());

                if (encodedData != null) {
                    query.append(encodedData);
                }

                Log.d(TAG, "doInBackground: query: " + query);

                // Write POST data.
                OutputStream outputStream = new BufferedOutputStream(httpsURLConnection.getOutputStream());
                BufferedWriter bufferedWriter = new BufferedWriter(
                        new OutputStreamWriter(outputStream, reference.getString(R.string.server_encoding)));
                bufferedWriter.write(query.toString());
                bufferedWriter.flush();
                bufferedWriter.close();

                setCookies(httpsURLConnection.getHeaderField(reference.getString(R.string.server_cookie_response_key)));

                // Retrieve the response.
                InputStream inputStream = new BufferedInputStream(httpsURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                Log.d(TAG, "populateView: done fetching data, the result is: \"" + stringBuilder.toString() + "\"");

                handleOutput(stringBuilder.toString());
            } catch (Exception e) {
                showInternalError(String.format(reference.getString(R.string.server_failure), reference.getString(R.string.server_hostname)), reference);
                e.printStackTrace();
            } finally {
                if (httpsURLConnection != null) {
                    httpsURLConnection.disconnect();
                }
            }
        } catch (final MalformedURLException e) {
            showInternalError(reference.getString(R.string.server_malformed_url), reference);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}