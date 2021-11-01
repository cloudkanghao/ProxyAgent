package com.kh.proxyagent.Fragments;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.kh.proxyagent.R;

import org.jetbrains.annotations.NotNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class HomeFragment extends Fragment {

    private View view;
    private ImageButton powerButton;
    private boolean toggle, variableSet;
    private String proxyAddress, port;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_home, container, false);

        powerButton = view.findViewById(R.id.powerButton);
        toggle = false;
        variableSet = false;
//        runtimePermission();

        SharedPreferences preferences = this.getActivity().getPreferences(Context.MODE_PRIVATE);

        proxyAddress = preferences.getString("proxyAddress", "");
        port = preferences.getString("port", "");

        Bundle bundle = this.getArguments();
        if (proxyAddress.equals("") && bundle != null)
            proxyAddress = bundle.getString("proxyAddress");
        if (port.equals("") && bundle != null)
            port = bundle.getString("port");

        if(!proxyAddress.equals("") && !port.equals("")) {
            variableSet = true;
//            Toast.makeText(getContext(), "port: " + port, Toast.LENGTH_SHORT).show();
        }

        powerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (variableSet) {
                        if (!toggle) {
//                            try {
//                                Runtime.getRuntime().exec("settings put global http_proxy " + proxyAddress + ":" + port);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
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
                    }
                    else
                        Toast.makeText(getContext(), "Proxy settings not set!", Toast.LENGTH_SHORT).show();
                }
            }
        );

        return view;
    }

    private void runtimePermission() {
        Dexter.withContext(getContext()).withPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).withListener(new MultiplePermissionsListener() {

            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }
}
