package aq.metallists.loudbang.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import aq.metallists.loudbang.LBService;
import aq.metallists.loudbang.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_SECTION_NUMBER = "section_number";
    BroadcastReceiver bs;
    private PageViewModel pageViewModel;
    private SharedPreferences sp;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    public void onResume() {
        super.onResume();

        try {
            final TextView state = this.getView().findViewById(R.id.statusLabel1);

            if (LBService.lastKnownState.length() > 0) {
                state.setText(LBService.lastKnownState);
            }
        } catch (Exception x) {
        }
    }

    private Timer tmr;

    public void onDestroyView() {
        if (tmr != null) {
            tmr.cancel();
            tmr.purge();
            tmr = null;
        }


        if (this.bs != null) {
            try {
                LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.bs);
            } catch (Exception x) {

            }
        }

        if (this.sp != null)
            this.sp.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroyView();
    }

    private final ActivityResultLauncher<Intent> mediaProjectionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        Toast.makeText(
                                this.getContext(),
                                R.string.sndinput_mediacapture_low_android_version,
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }
                    MediaProjectionManager mpm = (MediaProjectionManager) this.getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    Intent data = result.getData();
                    if (mpm == null || data == null) {
                        Toast.makeText(getContext(), R.string.sndinput_mediacapture_failed, Toast.LENGTH_LONG).show();
                        return;
                    }

                    data.setClass(getContext(), LBService.class);
                    data.putExtra("mprCode", result.getResultCode());
                    PlaceholderFragment.this.getActivity().startService(data);
                } else {
                    Toast.makeText(getContext(), R.string.sndinput_mediacapture_denied, Toast.LENGTH_LONG).show();
                    return;
                }
            });

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_status, container, false);

        final ProgressBar pbbm = root.findViewById(R.id.progressBar);
        final TextView state = root.findViewById(R.id.statusLabel1);
        final ToggleButton ltb = root.findViewById(R.id.launch_toggle_btn);
        final TextView lblTxNExt = root.findViewById(R.id.lblTransmitNextCount);

        if (!LBService.lastKnownState.isEmpty()) {
            state.setText(LBService.lastKnownState);
        }


        this.sp = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        this.bs = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.requireNonNull(intent.getAction()).contains("eme.eva.loudbang.state")) {
                    boolean isRunnin = isMyServiceRunning(LBService.class);
                    ltb.setChecked(isRunnin);

                    state.setText(intent.getStringExtra("eme.eva.loudbang.state"));
                } else {
                    pbbm.setProgress(intent.getIntExtra("eme.eva.loudbang.level", 50));
                }
            }
        };

        // eme.eva.loudbang.state
        IntentFilter intentActionFilter = new IntentFilter();
        intentActionFilter.addAction("eme.eva.loudbang.state");
        intentActionFilter.addAction("eme.eva.loudbang.recordlevel");

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(bs, intentActionFilter);


        ltb.setOnClickListener(v -> {
            List<String> permissionsToRequest = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                addPermissionToRequest(root,
                        permissionsToRequest, Manifest.permission.POST_NOTIFICATIONS);
            }

            //TODO: add a way to play-only
            addPermissionToRequest(root,
                    permissionsToRequest, Manifest.permission.RECORD_AUDIO);

            if (sp.getBoolean("use_gps", false)) {
                addPermissionToRequest(root,
                        permissionsToRequest, Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (sp.getBoolean("use_celltowers", false)) {
                addPermissionToRequest(root,
                        permissionsToRequest, Manifest.permission.ACCESS_COARSE_LOCATION);
            }


            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(PlaceholderFragment.this.getActivity(),
                        permissionsToRequest.toArray(new String[]{}),
                        0);

                ltb.setChecked(false);
                return;
            }


            if (!isMyServiceRunning(LBService.class)) {
                int txNextCtr = this.sp.getInt("tx_next_counter", 0);
                if (txNextCtr > 0) {
                    this.sp.edit().putInt("tx_next_counter", 0).apply();
                }

                if (sp.getBoolean("mediacapture_mode", false)) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        Toast.makeText(
                                this.getContext(),
                                R.string.sndinput_mediacapture_low_android_version,
                                Toast.LENGTH_LONG
                        ).show();
                        ltb.setChecked(false);
                        return;
                    }
                    MediaProjectionManager mpm = (MediaProjectionManager) this.getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                    Intent captureIntent = mpm.createScreenCaptureIntent();
                    mediaProjectionLauncher.launch(captureIntent);
                    ltb.setChecked(true);
                    return;
                }

                PlaceholderFragment.this.getActivity().startService(
                        new Intent(PlaceholderFragment.this.getActivity(), LBService.class));
                ltb.setChecked(true);
            } else {
                PlaceholderFragment.this.getActivity().stopService(
                        new Intent(PlaceholderFragment.this.getActivity(), LBService.class));
                ltb.setChecked(false);
            }
        });

        if (isMyServiceRunning(LBService.class)) {
            ltb.setChecked(true);
        }

        TextView bandname = root.findViewById(R.id.lbl_band);
        bandname.setText(this.getBandName(sp.getString("band", Double.toString(10.1387))));

        TextView callsign = root.findViewById(R.id.lbl_callsign);
        callsign.setText(this.sp.getString("callsign", "R0TST"));

        TextView grid = root.findViewById(R.id.lbl_grid);
        grid.setText(this.sp.getString("gridsq", "LO05io"));
        lblTxNExt.setText(Integer.toString(this.sp.getInt("tx_next_counter", 0)));

        TextView txstate = root.findViewById(R.id.lbl_txstate);
        String rxtx_state = "";
        if (this.sp.getBoolean("use_tx", false)) {
            rxtx_state = getString(R.string.lbl_tx_enabled);
        } else {
            rxtx_state = getString(R.string.lbl_tx_disabled);
        }

        Pattern cspatrn = Pattern.compile(
                "^[A-Z0-9]{1,3}/[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[A-Z0-9]$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[0-9]{2}$");

        if (!cspatrn.matcher(this.sp.getString("callsign", "XXX")).matches()) {
            rxtx_state = getString(R.string.lbl_tx_swlmode);
        }

        switch (this.sp.getString("ptt_ctl", "none")) {
            case "none":
                rxtx_state = rxtx_state + getString(R.string.tbt_txptt_noptt);
                break;
            case "fbang_2":
                rxtx_state = rxtx_state + getString(R.string.tbt_txptt_fc);
                break;
            case "fbang_1":
                rxtx_state = rxtx_state + getString(R.string.tbt_txptt_rc);
                break;
            default:
                rxtx_state = rxtx_state + ", <ERR>";
        }

        rxtx_state = String.format(Locale.GERMAN, "%s, %d%%", rxtx_state,
                Integer.parseInt(this.sp.getString("tx_probability", "25"))
        );

        txstate.setText(rxtx_state);


        //set tracking timer forsome systems
        final TextView lblCurrentTime = root.findViewById(R.id.lblCurrentTime);
        tmr = new Timer();
        tmr.schedule(new TimerTask() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                final String out = String.format(Locale.GERMANY, "%02d.%02d.%04d %02d:%02d:%02d",
                        cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR),
                        cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));

                Activity thisAct = getActivity();
                if (thisAct == null) {
                    Log.e("PHFrag", "thisAct is NULL!");
                    tmr.cancel();
                    tmr.purge();
                    return;
                }

                thisAct.runOnUiThread(() -> {
                    lblCurrentTime.setText(out);

                    ltb.setChecked(isMyServiceRunning(LBService.class));
                });
            }
        }, 1000, 1000);


        sp.registerOnSharedPreferenceChangeListener(this);
        return root;
    }

    private void addPermissionToRequest(final View view, final List<String> permissionsToRequest,
                                        final String permission) {
        if (ContextCompat.checkSelfPermission(view.getContext(), permission)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(permission);
        }
    }

    private String getBandName(String freq) {
        String[] freqs = this.getActivity().getResources().getStringArray(R.array.sets_bandarr_value);
        String[] bands = this.getActivity().getResources().getStringArray(R.array.sets_bandarr_name);

        if (freqs.length != bands.length) {
            return "<ERROR>";
        }

        for (int i = 0; i < freqs.length; i++) {
            if (freqs[i].equals(freq)) {
                return bands[i];
            }
        }

        return freq;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        Context ctx = this.getActivity();
        if (ctx == null) {
            return false;
        }

        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null)
            return false;

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            switch (key) {
                case "tx_next_counter":
                    TextView lblTxNExt = this.getView().findViewById(R.id.lblTransmitNextCount);
                    if (lblTxNExt != null)
                        lblTxNExt.setText(Integer.toString(this.sp.getInt("tx_next_counter", 0)));
                    break;
                case "band":
                    TextView bandname = this.getView().findViewById(R.id.lbl_band);
                    if (bandname != null)
                        bandname.setText(this.getBandName(
                                sp.getString("band", Double.toString(10.1387))));
                    break;
                case "callsign":
                    TextView callsign = this.getView().findViewById(R.id.lbl_callsign);
                    if (callsign != null)
                        callsign.setText(this.sp.getString("callsign", "R0TST"));
                    break;
                case "gridsq":
                    TextView grid = this.getView().findViewById(R.id.lbl_grid);
                    if (grid != null)
                        grid.setText(this.sp.getString("gridsq", "LO05io"));
                    break;
                case "use_tx":
                case "tx_probability":
                case "ptt_ctl":
                    TextView txstate = this.getView().findViewById(R.id.lbl_txstate);
                    if (txstate == null)
                        break;
                    String rxtx_state = "";
                    if (this.sp.getBoolean("use_tx", false)) {
                        rxtx_state = getString(R.string.lbl_tx_enabled);
                    } else {
                        rxtx_state = getString(R.string.lbl_tx_disabled);
                    }
                    Pattern cspatrn = Pattern.compile(
                            "^[A-Z0-9]{1,3}/[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[A-Z0-9]$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[0-9]{2}$");

                    if (!cspatrn.matcher(this.sp.getString("callsign", "XXX")).matches()) {
                        rxtx_state = getString(R.string.lbl_tx_swlmode);
                    }

                    switch (this.sp.getString("ptt_ctl", "none")) {
                        case "none":
                            rxtx_state = rxtx_state + getString(R.string.tbt_txptt_noptt);
                            break;
                        case "fbang_2":
                            rxtx_state = rxtx_state + getString(R.string.tbt_txptt_fc);
                            break;
                        case "fbang_1":
                            rxtx_state = rxtx_state + getString(R.string.tbt_txptt_rc);
                            break;
                        default:
                            rxtx_state = rxtx_state + ", <ERR>";
                    }

                    rxtx_state = String.format(Locale.GERMAN, "%s, %d%%", rxtx_state,
                            Integer.parseInt(this.sp.getString("tx_probability", "25"))
                    );

                    txstate.setText(rxtx_state);
                    break;
                default:
            }
        } catch (Exception x) {
            Log.e("ERROR", "Exception:", x);
        }
    }
}
