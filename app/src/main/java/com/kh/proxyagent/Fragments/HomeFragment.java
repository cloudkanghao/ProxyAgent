package com.kh.proxyagent.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kh.proxyagent.Foreground.ForegroundBuilder;
import com.kh.proxyagent.R;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

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
                        if(wifiConnected()) {
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
                                    startForegroundService();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.e("tag", e.getMessage());
                                }
                            } else {
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
                                    stopForegroundService();
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
                            Toast.makeText(getContext(), "Please connect to WiFi", Toast.LENGTH_SHORT).show();

                    }
                    else
                        Toast.makeText(getContext(), "Proxy settings not set!", Toast.LENGTH_SHORT).show();
                }
            }
        );

        return view;
    }

    private boolean wifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(getContext(), ForegroundBuilder.class);
        getContext().startService(serviceIntent);
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(getContext(), ForegroundBuilder.class);
        getContext().stopService(serviceIntent);
    }
}
