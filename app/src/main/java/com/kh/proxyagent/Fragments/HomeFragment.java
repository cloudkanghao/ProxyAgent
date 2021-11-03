package com.kh.proxyagent.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.kh.proxyagent.R;

import java.io.DataOutputStream;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private View view;
    private ImageButton powerButton;
    private boolean toggle, variableSet;
    private String proxyAddress, port;
    private final String VARIABLE_STATE = "variableSetState";
    private final String TOGGLE_STATE = "toggleState";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);

        powerButton = view.findViewById(R.id.powerButton);

        // Get saved state
        SharedPreferences preferences = this.getActivity().getPreferences(Context.MODE_PRIVATE);
        toggle = preferences.getBoolean(TOGGLE_STATE, false);
        variableSet = preferences.getBoolean(VARIABLE_STATE, false);

        if (toggle)
            powerButton.setImageResource(R.drawable.stop_button);

        proxyAddress = preferences.getString("proxyAddress", "");
        port = preferences.getString("port", "");

        Bundle bundle = this.getArguments();
        if (proxyAddress.equals("") && bundle != null)
            proxyAddress = bundle.getString("proxyAddress");
        if (port.equals("") && bundle != null)
            port = bundle.getString("port");

        if(!proxyAddress.equals("") && !port.equals(""))
            variableSet = true;

        // Set power button click listener
        powerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (variableSet) {
                        if (!toggle) {

                            try {
                                Process su = Runtime.getRuntime().exec("su");
                                DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                                outputStream.writeBytes("settings put global http_proxy " + proxyAddress + ":" + port + "\n");
                                outputStream.flush();

                                outputStream.writeBytes("exit\n");
                                outputStream.flush();
                                su.waitFor();
                                powerButton.setImageResource(R.drawable.stop_button);
                                toggle = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else {
//                            try {
//                                Runtime.getRuntime().exec("settings put global http_proxy :0");
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
                            try {
                                Process su = Runtime.getRuntime().exec("su");
                                DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                                outputStream.writeBytes("settings put global http_proxy :0\n");
                                outputStream.flush();

                                outputStream.writeBytes("exit\n");
                                outputStream.flush();
                                su.waitFor();
                                powerButton.setImageResource(R.drawable.power);
                                toggle = false;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // Ensure to save state!
                        SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();

                        editor.putBoolean(TOGGLE_STATE, toggle);
                        editor.putBoolean(VARIABLE_STATE, variableSet);
                        editor.commit();
                    }
                    else
                        Toast.makeText(getContext(), "Proxy settings not set!", Toast.LENGTH_SHORT).show();
                }
            }
        );

        return view;
    }

//    private void runtimePermission() {
//        Dexter.withContext(getContext()).withPermissions(
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//        ).withListener(new MultiplePermissionsListener() {
//
//            @Override
//            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
//
//            }
//
//            @Override
//            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
//                permissionToken.continuePermissionRequest();
//            }
//        }).check();
//    }

//    @Override
//    public void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putBoolean(TOGGLE_STATE, toggle);
//        outState.putBoolean(VARIABLE_STATE, variableSet);
//        Toast.makeText(getContext(), "save state toggle: " + toggle, Toast.LENGTH_SHORT).show();
//    }
}
