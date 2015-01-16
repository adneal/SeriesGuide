package com.battlelancer.seriesguide.migration;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

/**
 * Helps the user install or open SeriesGuide.
 */
public class MigrationActivity extends ActionBarActivity {

    private static final String MARKETLINK_HTTP
            = "http://play.google.com/store/apps/details?id=com.battlelancer.seriesguide";
    private static final String PACKAGE_SERIESGUIDE = "com.battlelancer.seriesguide";

    private Button mButtonLaunch;
    private TextView mTextViewLaunchInstructions;

    private Intent mLaunchIntentForPackage;

    private View.OnClickListener mSeriesGuideLaunchListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mLaunchIntentForPackage != null) {
                startActivity(mLaunchIntentForPackage);
            }
        }
    };
    private View.OnClickListener mSeriesGuideInstallListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // launch SeriesGuide Play Store page
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKETLINK_HTTP));
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
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void setupViews() {
        mTextViewLaunchInstructions = (TextView) findViewById(
                R.id.textViewMigrationLaunchInstructions);
        mButtonLaunch = (Button) findViewById(R.id.buttonMigrationLaunch);

        findViewById(R.id.buttonMigrationHideLauncher).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        hideLauncherIcon();
                    }
                }
        );
    }

    @Override
    protected void onStart() {
        super.onStart();
        validateLaunchStep();
    }

    private void validateLaunchStep() {
        // check if SeriesGuide is already installed
        PackageManager packageManager = getPackageManager();
        mLaunchIntentForPackage = packageManager == null ? null
                : packageManager.getLaunchIntentForPackage(PACKAGE_SERIESGUIDE);
        boolean isSeriesGuideInstalled = mLaunchIntentForPackage != null;

        // enable install or open functionality
        mTextViewLaunchInstructions.setText(
                isSeriesGuideInstalled ? R.string.migration_launch : R.string.migration_install);
        mButtonLaunch.setText(isSeriesGuideInstalled ? R.string.migration_action_launch
                : R.string.migration_action_install);
        mButtonLaunch.setOnClickListener(
                isSeriesGuideInstalled ? mSeriesGuideLaunchListener : mSeriesGuideInstallListener);
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
}
