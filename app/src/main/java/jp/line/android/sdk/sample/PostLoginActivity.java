package jp.line.android.sdk.sample;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
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

    private static final String TAG = "PostLoginActivity";

    private LineApiClient lineApiClient;

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

            @Override
            public void onClick(View v) {
                new GetProfileTask().execute();
            }
        });

        // Refresh Button Click Listener
        final Button refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new RefreshTokenTask().execute();
            }
        });

        // Verify Button Click Listener
        final Button verifyButton = findViewById(R.id.verifyButton);
        verifyButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                new VerifyTokenTask().execute();
            }
        });

        // Logout Button Click Listener
        final Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {

            @Override
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
            Log.i(TAG, "Picture URL: " + pictureUrl);
            new ImageLoaderTask().execute(pictureUrl.toString());
        }

        final TextView displayNameField = findViewById(R.id.displayNameField);
        displayNameField.setText(intentProfile.getDisplayName());

        final TextView userIdField = findViewById(R.id.userIDField);
        userIdField.setText(intentProfile.getUserId());

        final TextView statusMessageField = findViewById(R.id.statusMessageField);
        statusMessageField.setText(intentProfile.getStatusMessage());

        final TextView accessTokenField = findViewById(R.id.accessTokenField);
        accessTokenField.setText(intentCredential.getAccessToken().getAccessToken());
    }

    public static class ProfileDialogFragment extends DialogFragment {

        private LineProfile profileInfo;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.profile_dialog, null);

            final TextView profileName = view.findViewById(R.id.profileName);
            profileName.setText(profileInfo.getDisplayName());
            final TextView profileMessage = view.findViewById(R.id.profileMessage);
            profileMessage.setText(profileInfo.getStatusMessage());
            final TextView profileMid = view.findViewById(R.id.profileMid);
            profileMid.setText(profileInfo.getUserId());

            Uri pictureUrl = profileInfo.getPictureUrl();
            final TextView profileImage = view.findViewById(R.id.profileImageUrl);

            // If the user's profile picture is not set, the picture url will be null.
            if (pictureUrl != null) {
                profileImage.setText(pictureUrl.toString());
            } else {
                profileImage.setText(view.getContext().getResources().getString(R.string.no_profile_image_set));
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

        private static final String TAG = "ImageLoaderTask";

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
        }
    }

    public class RefreshTokenTask extends AsyncTask<Void, Void, LineApiResponse<LineAccessToken>> {

        private static final String TAG = "RefreshTokenTask";

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
        }
    }

    public class VerifyTokenTask extends AsyncTask<Void, Void, LineApiResponse<LineCredential>> {

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
        }
    }

    public class GetProfileTask extends AsyncTask<Void, Void, LineApiResponse<LineProfile>> {

        private static final String TAG = "GetProfileTask";

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
            } else {
                Toast.makeText(getApplicationContext(), "Failed to get profile.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to get Profile: " + apiResponse.getErrorData());
            }
        }
    }

    public class LogoutTask extends AsyncTask<Void, Void, LineApiResponse> {

        private static final String TAG = "LogoutTask";

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
                Log.e(TAG, "Logout Failed: " + apiResponse.getErrorData());
            }
        }
    }
}
