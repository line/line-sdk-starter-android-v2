package jp.line.android.sdk.sample;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.linecorp.linesdk.LineAccessToken;
import com.linecorp.linesdk.LineApiResponse;
import com.linecorp.linesdk.LineCredential;
import com.linecorp.linesdk.LineProfile;
import com.linecorp.linesdk.api.LineApiClient;
import com.linecorp.linesdk.api.LineApiClientBuilder;

public class PostLoginActivity extends AppCompatActivity {

    private LineApiClient lineApiClient;

    // Method for preventing orientation changes during ASyncTasks
    private void lockScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    // This method is used to reenable orientation changes after an ASyncTask is finished.
    private void unlockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_login);

        LineApiClientBuilder apiClientBuilder = new LineApiClientBuilder(getApplicationContext(),
                                                                         Constants.CHANNEL_ID);
        lineApiClient = apiClientBuilder.build();

        // Profile Button Click Listener
        final Button profileButton = findViewById(R.id.profileButton);

        profileButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new GetProfileTask().execute();
            }
        });

        // Refresh Button Click Listener
        final Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new RefreshTokenTask().execute();
            }

        });

        // Verify Button Click Listener
        final Button verifyButton = findViewById(R.id.verifyButton);
        verifyButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new VerifyTokenTask().execute();
            }
        });

        // Logout Button Click Listener
        final Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                new LogoutTask().execute();
                finish();
            }
        });

        // Get the intent so that we can get information from the previous activity
        Intent intent = getIntent();
        LineProfile intentProfile = intent.getParcelableExtra("line_profile");
        LineCredential intentCredential = intent.getParcelableExtra("line_credential");

        Uri pictureUrl = intentProfile.getPictureUrl();

        if (pictureUrl != null) {
            Log.i("PostLoginActivity", "Picture URL: " + pictureUrl.toString());
            new ImageLoaderTask().execute(pictureUrl.toString());
        }

        TextView profileText;

        profileText = findViewById(R.id.displayNameField);
        profileText.setText(intentProfile.getDisplayName());

        profileText = findViewById(R.id.userIDField);
        profileText.setText(intentProfile.getUserId());

        profileText = findViewById(R.id.statusMessageField);
        profileText.setText(intentProfile.getUserId());

        profileText = findViewById(R.id.accessTokenField);
        profileText.setText(intentCredential.getAccessToken().getAccessToken());
    }

    public static class ProfileDialogFragment extends DialogFragment {

        private LineProfile profileInfo;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.profile_dialog, null);

            TextView textview = view.findViewById(R.id.profileName);
            textview.setText(profileInfo.getDisplayName());
            textview = view.findViewById(R.id.profileMessage);
            textview.setText(profileInfo.getStatusMessage());
            textview = view.findViewById(R.id.profileMid);
            textview.setText(profileInfo.getUserId());
            Uri pictureUrl = profileInfo.getPictureUrl();
            textview = view.findViewById(R.id.profileImageUrl);

            // If the user's profile picture is not set, the picture url will be null.
            if (pictureUrl != null) {
                textview.setText(profileInfo.getPictureUrl().toString());
            } else {
                textview.setText(view.getContext().getResources().getString(R.string.no_profile_image_set));
            }

            view.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            view.findViewById(R.id.profileContent).setVisibility(View.VISIBLE);

            builder.setView(view);
            builder.setPositiveButton("OK", null);
            return builder.create();
        }

        public LineProfile getProfileInfo() {
            return profileInfo;
        }

        public void setProfileInfo(LineProfile profileInfo) {
            this.profileInfo = profileInfo;
        }
    }

    public class ImageLoaderTask extends AsyncTask<String, String, Bitmap> {

        static final String TAG = "ImageLoaderTask";

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap = null;
            try {
                URL url = new URL(strings[0]);
                bitmap = BitmapFactory.decodeStream((InputStream) url.getContent());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView profileImageView = findViewById(R.id.profileImageView);
            profileImageView.setImageBitmap(bitmap);
            unlockScreenOrientation();
        }
    }

    public class RefreshTokenTask extends AsyncTask<Void, Void, LineApiResponse<LineAccessToken>> {

        static final String TAG = "RefreshTokenTask";

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
        }

        @Override
        protected LineApiResponse<LineAccessToken> doInBackground(Void... params) {

            return lineApiClient.refreshAccessToken();
        }

        @Override
        protected void onPostExecute(LineApiResponse<LineAccessToken> response) {
            if (response.isSuccess()) {
                String updatedAccessToken =
                        lineApiClient.getCurrentAccessToken().getResponseData().getAccessToken();

                // Update the view
                TextView accessTokenField = findViewById(R.id.accessTokenField);
                accessTokenField.setText(updatedAccessToken);
                Toast.makeText(getApplicationContext(), "Access Token has been refreshed.", Toast.LENGTH_SHORT)
                     .show();
            } else {
                Toast.makeText(getApplicationContext(), "Could not refresh the access token.",
                               Toast.LENGTH_SHORT).show();
                Log.e(TAG, response.getErrorData().toString());
            }

            unlockScreenOrientation();
        }
    }

    public class VerifyTokenTask extends AsyncTask<Void, Void, LineApiResponse<LineCredential>> {

        static final String TAG = "VerifyTokenTask";

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
        }

        @Override
        protected LineApiResponse<LineCredential> doInBackground(Void... params) {
            return lineApiClient.verifyToken();
        }

        @Override
        protected void onPostExecute(LineApiResponse<LineCredential> response) {
            if (response.isSuccess()) {
                StringBuilder toastStringBuilder = new StringBuilder(
                        "Access Token is VALID and contains the permissions ");

                for (String temp : response.getResponseData().getPermission()) {
                    toastStringBuilder.append(temp + ", ");
                }
                Toast.makeText(getApplicationContext(), toastStringBuilder.toString(), Toast.LENGTH_SHORT)
                     .show();

            } else {
                Toast.makeText(getApplicationContext(), "Access Token is NOT VALID", Toast.LENGTH_SHORT).show();
            }
            unlockScreenOrientation();
        }
    }

    public class GetProfileTask extends AsyncTask<Void, Void, LineApiResponse<LineProfile>> {

        static final String TAG = "GetProfileTask";

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
        }

        @Override
        protected LineApiResponse<LineProfile> doInBackground(Void... params) {
            return lineApiClient.getProfile();
        }

        @Override
        protected void onPostExecute(LineApiResponse<LineProfile> apiResponse) {
            if (apiResponse.isSuccess()) {
                ProfileDialogFragment newFragment = new ProfileDialogFragment();
                newFragment.setProfileInfo(apiResponse.getResponseData());
                newFragment.show(getFragmentManager(), null);
                unlockScreenOrientation();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to get profile.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to get Profile: " + apiResponse.getErrorData().toString());
            }
        }
    }

    public class LogoutTask extends AsyncTask<Void, Void, LineApiResponse> {

        static final String TAG = "LogoutTask";

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
        }

        @Override
        protected LineApiResponse doInBackground(Void... params) {
            return lineApiClient.logout();
        }

        @Override
        protected void onPostExecute(LineApiResponse apiResponse) {
            if (apiResponse.isSuccess()) {
                Toast.makeText(getApplicationContext(), "Logout Successful", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Logout Failed", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Logout Failed: " + apiResponse.getErrorData().toString());
            }
            unlockScreenOrientation();
        }
    }
}
