package jp.line.android.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.linecorp.linesdk.auth.LineLoginApi;
import com.linecorp.linesdk.auth.LineLoginResult;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 1;

    private static final String TAG = "ERROR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView appToAppButton = findViewById(R.id.login_button);
        appToAppButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    // App to App Login
                    Intent LoginIntent = LineLoginApi.getLoginIntent(v.getContext(), Constants.CHANNEL_ID);
                    startActivityForResult(LoginIntent, REQUEST_CODE);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });

        final TextView browserLoginButton = findViewById(R.id.browser_login_button);
        browserLoginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    // Browser Login
                    Intent LoginIntent = LineLoginApi.getLoginIntentWithoutLineAppAuth(v.getContext(),
                                                                                       Constants.CHANNEL_ID);
                    startActivityForResult(LoginIntent, REQUEST_CODE);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unsupported Request");
            return;
        }

        LineLoginResult result = LineLoginApi.getLoginResultFromIntent(data);

        switch (result.getResponseCode()) {
            case SUCCESS:
                Intent transitionIntent = new Intent(this, PostLoginActivity.class);
                transitionIntent.putExtra("line_profile", result.getLineProfile());
                transitionIntent.putExtra("line_credential", result.getLineCredential());
                startActivity(transitionIntent);
                break;

            case CANCEL:
                Log.e(TAG, "LINE Login Canceled by user!!");
                break;

            default:
                Log.e(TAG, "Login FAILED!");
                Log.e(TAG, result.getErrorData().toString());
        }
    }
}
