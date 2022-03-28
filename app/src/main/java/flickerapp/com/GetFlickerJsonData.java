package flickerapp.com;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class GetFlickerJsonData extends AsyncTask<String, Void, List<Photo>> implements GetRawData.OnDownloadComplete {
    private static final String TAG = "GetFlickerJsonData";

    private List<Photo> mPhotoList = null;
    private String mBaseURL;
    private String mLanguage;
    private boolean mMatchAll;

    private final OnDataAvailable mCallBack;
    private boolean runningOnSameThread = false;

    interface OnDataAvailable{
        void onDataAvailable(List<Photo> data, DownloadStatus status);
    }


    public GetFlickerJsonData(OnDataAvailable mCallBack, String mBaseURL, String mLanguage, boolean mMatchAll) {
        Log.d(TAG, "GetFlickerJsonData: called CONSTRUCTOR");
        this.mBaseURL = mBaseURL;
        this.mLanguage = mLanguage;
        this.mMatchAll = mMatchAll;
        this.mCallBack = mCallBack;
    }

    void executeOnSameThread(String searchCriteria) {
        Log.d(TAG, "executeOnSameThread: starts");

        runningOnSameThread = true;
        String destinationUri = createUri(searchCriteria, mLanguage, mMatchAll);

        GetRawData getRawData = new GetRawData(this);
        getRawData.execute(destinationUri);
        Log.d(TAG, "executeOnSameThread:  ends");
    }

    @Override
    protected void onPostExecute(List<Photo> photos) {
        Log.d(TAG, "onPostExecute: STARTS");

        if (mCallBack != null) {
            mCallBack.onDataAvailable(mPhotoList, DownloadStatus.OK);
        }
        Log.d(TAG, "onPostExecute: ENDS");
    }

    @Override
    protected List<Photo> doInBackground(String... params) {
        Log.d(TAG, "doInBackground: starts");
        String destinationURI = createUri(params[0], mLanguage, mMatchAll);

        GetRawData getRawData = new GetRawData(this);
        getRawData.runInSameThread(destinationURI);
        Log.d(TAG, "doInBackground:  ends");
        return mPhotoList;
    }

    private String createUri(String searchCriteria, String language, boolean matchAll) {
        Log.d(TAG, "createUri: starts");

//        Uri uri = Uri.parse(mBaseURL).buildUpon()
//                .appendQueryParameter("tags", searchCriteria)
//                .appendQueryParameter("tagmode", matchAll ? "ALL" : "ANY")
//                .appendQueryParameter("lang", language).appendQueryParameter("format", "json")
//                .appendQueryParameter("nojsoncallback", "1")
//                .build();

        return Uri.parse(mBaseURL).buildUpon()
                .appendQueryParameter("tags", searchCriteria)
                .appendQueryParameter("tagmode", matchAll ? "ALL" : "ANY")
                .appendQueryParameter("lang", language)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("nojsoncallback", "1")
                .build().toString();
    }


    @Override
    public void onDownloadComplete(String data, DownloadStatus status) {
        Log.d(TAG, "onDownloadComplete: starts. Status = " + status);

        if (status == DownloadStatus.OK) {
            mPhotoList = new ArrayList<>();

            try {
                JSONObject jsonData = new JSONObject(data);
                JSONArray itemsArray = jsonData.getJSONArray("items");

                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject jsonPhoto = itemsArray.getJSONObject(i);
                    String title = jsonPhoto.getString("title");
                    String author = jsonPhoto.getString("author");
                    String authorID = jsonPhoto.getString("author_id");
                    String tags = jsonPhoto.getString("tags");

                    JSONObject jsonObjectMedia = jsonPhoto.getJSONObject("media");
                    String photoURL = jsonObjectMedia.getString("m");

                    String link = photoURL.replaceFirst("_m.", "_b.");

                    Photo photoOBJ = new Photo(title, author, authorID, link, tags, photoURL);
                    mPhotoList.add(photoOBJ);

                    Log.d(TAG, "onDownloadComplete: " + photoOBJ.toString());
                }
            } catch (JSONException jsonException) {
                jsonException.printStackTrace();
                Log.e(TAG, "onDownloadComplete: Error processing JSON data" + jsonException.getMessage());
                status = DownloadStatus.FAILED_OR_EMPTY;
            }
        }

        if (runningOnSameThread && mCallBack != null) {
            //now inform the caller that processing is done = possible returning null if there was
            // an error
            mCallBack.onDataAvailable(mPhotoList, status);
        }
        Log.d(TAG, "onDownloadComplete: ends");
    }
}







