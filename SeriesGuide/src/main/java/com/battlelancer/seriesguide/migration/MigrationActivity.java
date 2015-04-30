package com.battlelancer.seriesguide.migration;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.battlelancer.seriesguide.licensing.AESObfuscator;
import com.battlelancer.seriesguide.licensing.LicenseChecker;
import com.battlelancer.seriesguide.licensing.Policy;
import com.battlelancer.seriesguide.licensing.ServerManagedPolicy;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

/**
 * Helps the user install or open SeriesGuide.
 */
public class MigrationActivity extends AppCompatActivity {

    private static final String BASE64_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmVrsSyPfNLUE6ng68B/gPqnpUxlyPhLIX4sxqXT"
                    + "yylff7dWeyqrDOixzVWmEKb8BSRQVWPPR7RDF69sWO8qfsKTPJ5AzjFFc2NNfnjmvXmZnrVHrJKb"
                    + "xtIRLG/cxoiYu7q5vTbMF3YlwcxxghAZxBnZCxRphR/XnHxPQadK5YnemlGencFro3TnnbVnwlDU"
                    + "6La71QMI1rxIFM/xgB4uKs8CNyAXcwjHf+9X2fLs/afspXy/USS+bhA0zuYNakLJy1l6PlfZr4Jh"
                    + "nlUV0d8U4oazm3k99wF0BFOnI53hKp42kgLE8MKgB5cEIaVLJnbdyl2vwd+XYo6NASjKgpl7xxwI"
                    + "DAQAB";

    // Generate your own 20 random bytes, and put them here.
    private static final byte[] SALT = new byte[] {
            -28, -80, -102, 55, 28,
            98, -109, 101, -73, 77,
            63, -74, 110, 81, 18,
            -43, 48, -69, 67, -29
    };

    private static final String MARKETLINK_SERIESGUIDE_HTTP
            = "http://play.google.com/store/apps/details?id=com.battlelancer.seriesguide";
    private static final String MARKETLINK_XPASS_HTTP
            = "http://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.x";
    private static final String PACKAGE_SERIESGUIDE = "com.battlelancer.seriesguide";
    private static final String SUPPORT_MAIL = "uwe@seriesgui.de";

    private Button mButtonLaunch;
    private TextView mTextViewInstructions;
    private View mProgressBar;

    private Intent mLaunchIntentForPackage;

    private Handler mHandler;
    private LicenseChecker mChecker;
    private LicenseCheckerCallback mLicenseCheckerCallback;

    private View.OnClickListener mSeriesGuideLaunchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mLaunchIntentForPackage != null) {
                startActivity(mLaunchIntentForPackage);
            }
        }
    };
    private View.OnClickListener mRetryLicenseCheckListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            doLicenseCheck();
        }
    };
    private View.OnClickListener mSeriesGuideInstallListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // launch SeriesGuide Play Store page
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKETLINK_SERIESGUIDE_HTTP));
            Utils.tryStartActivity(MigrationActivity.this, intent, true);
        }
    };
    private View.OnClickListener mXPassInstallListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // launch X Pass Play Store page
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKETLINK_XPASS_HTTP));
            Utils.tryStartActivity(MigrationActivity.this, intent, true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(R.style.SeriesGuideTheme);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_migration);

        setupActionBar();
        setupViews();

        setupLicenseCheck();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(R.string.title);
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupViews() {
        mTextViewInstructions = (TextView) findViewById(
                R.id.textViewMigrationLaunchInstructions);
        mButtonLaunch = (Button) findViewById(R.id.buttonMigrationLaunch);
        mProgressBar = findViewById(R.id.progressBarLicenseCheck);

        findViewById(R.id.buttonMigrationHideLauncher).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideLauncherIcon();
                    }
                }
        );

        setProgressVisibility(true);
    }

    private void setupLicenseCheck() {
        // Might want to add further data in the future
        String deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        mHandler = new Handler();
        mLicenseCheckerCallback = new LicenseCheckerCallback();

        // Construct the LicenseChecker with a Policy.
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        doLicenseCheck();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mChecker.onDestroy();
    }

    private void doLicenseCheck() {
        setProgressVisibility(true);
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private void updateViews(final String result) {
        switch (result) {
            case LicenseCheckerCallback.FAIL:
                // not licensed
                mTextViewInstructions.setText(R.string.licence_check_fail);
                mTextViewInstructions.setTextAppearance(this,
                        R.style.TextAppearance_SeriesGuide_Red);
                mButtonLaunch.setText(R.string.action_buy_xpass);
                mButtonLaunch.setOnClickListener(mXPassInstallListener);
                break;
            case LicenseCheckerCallback.RETRY:
                // license check failed due to network issues
                mTextViewInstructions.setText(R.string.license_check_retry);
                mTextViewInstructions.setTextAppearance(this,
                        R.style.TextAppearance_SeriesGuide_Red);
                mButtonLaunch.setText(R.string.action_license_check);
                mButtonLaunch.setOnClickListener(mRetryLicenseCheckListener);
                break;
            case LicenseCheckerCallback.ALLOW:
                // licensed
                // check if SeriesGuide is already installed
                PackageManager packageManager = getPackageManager();
                mLaunchIntentForPackage = packageManager == null ? null
                        : packageManager.getLaunchIntentForPackage(PACKAGE_SERIESGUIDE);
                boolean isSeriesGuideInstalled = mLaunchIntentForPackage != null;

                // enable install or open functionality
                mTextViewInstructions.setText(
                        isSeriesGuideInstalled ? R.string.migration_launch
                                : R.string.migration_install);
                mTextViewInstructions.setTextAppearance(this,
                        R.style.TextAppearance_AppCompat_Body1);
                mButtonLaunch.setText(isSeriesGuideInstalled ? R.string.migration_action_launch
                        : R.string.migration_action_install);
                mButtonLaunch.setOnClickListener(
                        isSeriesGuideInstalled ? mSeriesGuideLaunchListener
                                : mSeriesGuideInstallListener);
                break;
            default:
                // application error when checking for license
                final String errorMessage = getString(R.string.licence_check_error, result);
                mTextViewInstructions.setText(errorMessage);
                mTextViewInstructions.setTextAppearance(this,
                        R.style.TextAppearance_SeriesGuide_Red);
                mButtonLaunch.setText(R.string.action_report_error);
                mButtonLaunch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendEmail(errorMessage);
                    }
                });
                break;
        }
    }

    private void hideLauncherIcon() {
        PackageManager p = getPackageManager();
        if (p != null) {
            p.setComponentEnabledSetting(getComponentName(),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            Toast.makeText(this, R.string.hide_confirmation, Toast.LENGTH_LONG).show();
        }
    }

    private void sendEmail(String messageBody) {
        final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                SUPPORT_MAIL
        });
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "X Pass Error");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, messageBody);

        Intent chooser = Intent.createChooser(intent, getString(R.string.action_report_error));
        Utils.tryStartActivity(this, chooser, true);
    }

    private void setProgressVisibility(boolean visible) {
        mButtonLaunch.setVisibility(visible ? View.GONE : View.VISIBLE);
        mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private class LicenseCheckerCallback
            implements com.battlelancer.seriesguide.licensing.LicenseCheckerCallback {
        public static final String ALLOW = "allow";
        public static final String RETRY = "retry";
        public static final String FAIL = "fail";

        public void allow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            postResult("allow");
        }

        public void dontAllow(int policyReason) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            postResult(policyReason == Policy.RETRY ? RETRY : FAIL);
        }

        public void applicationError(int errorCode) {
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            postResult(String.valueOf(errorCode));
        }
    }

    private void postResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {
                updateViews(result);
                setProgressVisibility(false);
            }
        });
    }
}
