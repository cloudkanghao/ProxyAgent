package com.kh.proxyagent.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kh.proxyagent.MainActivity;
import com.kh.proxyagent.R;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends Fragment {

    private View view;
    private EditText proxyAddress, port;
    private Button saveButton;
    private boolean doNotShowAgain;
    private final String TOGGLE_STATE = "toggleState";

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting, container, false);
        proxyAddress = view.findViewById(R.id.proxyAddress);
        port = view.findViewById(R.id.port);
        saveButton = view.findViewById(R.id.settingSubmit);

        SharedPreferences preferences = this.getActivity().getPreferences(MODE_PRIVATE);

        String proxyAddressValue = preferences.getString("proxyAddress", "");
        String portValue = preferences.getString("port", "");
        doNotShowAgain = preferences.getBoolean("doNotShowAgain", false);
        proxyAddress.setText(proxyAddressValue);
        port.setText(portValue);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String proxyAddressValue = proxyAddress.getText().toString(), portValue = port.getText().toString();
                boolean validPort = true;
                try {
                    int intValue = Integer.parseInt(portValue);
                } catch (NumberFormatException e) {
                    validPort = false;
                }

                if(wifiConnected()) {
                    if (Patterns.IP_ADDRESS.matcher(proxyAddressValue).matches() && validPort) {

                        SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();

                        editor.putString("proxyAddress", proxyAddressValue);
                        editor.putString("port", portValue);
                        editor.commit();

                        Bundle bundle = new Bundle();
                        bundle.putString("proxyAddress", proxyAddressValue);
                        bundle.putString("port", portValue);
                        HomeFragment homeFragment = new HomeFragment();
                        homeFragment.setArguments(bundle);
                        getFragmentManager().beginTransaction().replace(R.id.fragment_container, homeFragment).commit();

                        if (certificateIsImported())
                            Toast.makeText(getContext(), "Setting updated!", Toast.LENGTH_SHORT).show();
                        else {
                            if(!doNotShowAgain) {
                                AlertDialog.Builder mBuilder = new AlertDialog.Builder(getContext());
                                View mView = getLayoutInflater().inflate(R.layout.cert_dialog, null);

                                Button yes = mView.findViewById(R.id.yesButton);
                                Button cancel = mView.findViewById(R.id.cancelButton);
                                CheckBox check = mView.findViewById(R.id.checkBox);

                                mBuilder.setView(mView);
                                AlertDialog dialog = mBuilder.create();

                                yes.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        dialog.dismiss();
                                        proxySetting(true);
                                        installCertificate();
                                    }
                                });
                                cancel.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(getContext(), "Operation cancelled", Toast.LENGTH_SHORT).show();
                                        if(check.isChecked()) {
                                            doNotShowAgain = true;
                                            editor.putBoolean("doNotShowAgain", doNotShowAgain);
                                            editor.commit();
                                        }
                                        dialog.dismiss();
                                    }
                                });
                                dialog.show();
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "invalid IP or port", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(getContext(), "Please connect to WiFi", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }

    private boolean wifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }

    private boolean certificateIsImported() {
        File cert = new File(getContext().getFilesDir(), "burp.der");
        if (cert.exists())
            return true;
        else
            return false;
    }

    private void proxySetting(boolean on) {
        if (on) {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                outputStream.writeBytes("settings put global http_proxy " + proxyAddress.getText().toString() + ":" + port.getText().toString() + "\n");
                outputStream.flush();

                outputStream.writeBytes("exit\n");
                outputStream.flush();
                su.waitFor();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                outputStream.writeBytes("settings put global http_proxy :0\n");
                outputStream.flush();

                outputStream.writeBytes("exit\n");
                outputStream.flush();
                su.waitFor();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void installCertificate() {

        String url = "http://burp/cert";
        Request request = new Request.Builder().url(url).build();
        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()) {
                    byte[] res = response.body().bytes();

                    saveBurpDerFile(res);
                    convertDerToPem();
                    proxySetting(false);
                    if(moveCertToRootAuthority()) {
                        try {
                            Process su = Runtime.getRuntime().exec("su");
                            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                            outputStream.writeBytes("reboot\n");
                            outputStream.flush();

                            outputStream.writeBytes("exit\n");
                            outputStream.flush();
                            su.waitFor();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        Log.e("thread", "ERROR IMPORTING CERT!");
                }
            }
        });
    }

    private String convertToBase64(File file) throws IOException {

        InputStream inputStream = null;//You can get an inputStream using any IO API
        inputStream = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Base64OutputStream output64 = new Base64OutputStream(output, Base64.DEFAULT);
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output64.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        output64.close();

        return output.toString();
    }

    private void saveBurpDerFile(byte[] res) throws IOException {

        FileOutputStream fos = null;

        fos = getActivity().openFileOutput("burp.der", MODE_PRIVATE);
        fos.write(res);
    }

    private void convertDerToPem() throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        fis = getActivity().openFileInput("burp.der");
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String text;

        while ((text = br.readLine()) != null) {
            sb.append(text).append("\n");
        }

        Log.i("tag", "saved at:" + getContext().getFilesDir());

        String top = "-----BEGIN CERTIFICATE-----\n", bottom = "-----END CERTIFICATE-----\n";
        File file = new File(getContext().getFilesDir(), "burp.der");
        String pem = convertToBase64(file);
        pem = top + pem;
        pem += bottom;
        fos = getContext().openFileOutput("burp.pem", MODE_PRIVATE);
        fos.write(pem.getBytes());
    }

    private boolean moveCertToRootAuthority() {
        //9a5ba575.0
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("mv " + getContext().getFilesDir() + "/burp.pem /system/etc/security/cacerts/9a5ba575.0\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();

            Process su2 = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream2 = new DataOutputStream(su2.getOutputStream());

            outputStream2.writeBytes("chmod 644 /system/etc/security/cacerts/9a5ba575.0\n");
            outputStream2.flush();

            outputStream2.writeBytes("exit\n");
            outputStream2.flush();
            su2.waitFor();
            return true;
//            Toast.makeText(getContext(), "Certificate imported successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

//    private boolean runCommand(String command) {
//        try {
//            Process su = Runtime.getRuntime().exec("su");
//            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
//
//            outputStream.writeBytes(command + "\n");
//            outputStream.flush();
//
//            outputStream.writeBytes("exit\n");
//            outputStream.flush();
//            su.waitFor();
//            return true;
//        } catch (IOException | InterruptedException e) {
//            return false;
//        }
//    }
}
