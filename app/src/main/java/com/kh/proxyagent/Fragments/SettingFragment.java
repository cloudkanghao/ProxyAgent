/*
 * Copyright (c) 2021 cloud_kh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kh.proxyagent.Fragments;

import android.annotation.SuppressLint;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kh.proxyagent.HttpAsync.CallbackFuture;
import com.kh.proxyagent.MainActivity;
import com.kh.proxyagent.R;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends Fragment {

    private View view;
    private EditText port;
    private AutoCompleteTextView proxyAddress;
    private Button saveButton;
    private TextView certImportText;
    private ImageView certImportImage;
    private boolean doNotShowAgain;
    private final String TOGGLE_STATE = "toggleState",
            CHECK_STATE = "connectState",
            INTERFACE_STATE = "interfaceState",
            HISTORY = "history";

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting, container, false);
        proxyAddress = view.findViewById(R.id.proxyAddress);
        port = view.findViewById(R.id.port);
        saveButton = view.findViewById(R.id.settingSubmit);
        certImportText = view.findViewById(R.id.certImportText);
        certImportImage = view.findViewById(R.id.certImportImage);

        SharedPreferences preferences = this.getActivity().getPreferences(MODE_PRIVATE);

        // reserved
        String proxyAddressValue = preferences.getString("proxyAddress", "");
        String portValue = preferences.getString("port", "");
        doNotShowAgain = preferences.getBoolean("doNotShowAgain", false);
        ArrayList<String> historyList = getHistory(preferences);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, (List) historyList);
        proxyAddress.setAdapter(adapter);
        proxyAddress.setText(proxyAddressValue);
        proxyAddress.setThreshold(0);
        proxyAddress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    proxyAddress.showDropDown();
                }
                return false;
            }
        });
        port.setText(portValue);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String proxyAddressValue = proxyAddress.getText().toString(), portValue = port.getText().toString();

                if(!historyList.contains((proxyAddressValue)))
                    historyList.add(proxyAddressValue);
                boolean validPort = true;
                try {
                    Integer.parseInt(portValue);
                } catch (NumberFormatException e) {
                    validPort = false;
                }

                if (Patterns.IP_ADDRESS.matcher(proxyAddressValue).matches() && validPort) {

                    SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();

                    editor.putString("proxyAddress", proxyAddressValue);
                    editor.putString(HISTORY, historyListToString(historyList));
                    editor.putString("port", portValue);
                    editor.putBoolean(CHECK_STATE, false);
                    editor.putString(INTERFACE_STATE, proxyAddressValue + ":" + portValue);
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
                        if (!doNotShowAgain) {
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
                                    installCertificate();
                                }
                            });
                            cancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (check.isChecked()) {
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
                }
                // if ip/port is incorrect
                else {
                    Toast.makeText(getContext(), "invalid IP or port", Toast.LENGTH_SHORT).show();
                    SharedPreferences preferences = getActivity().getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(CHECK_STATE, false);
                    editor.putString(INTERFACE_STATE, "Not connected");
                    editor.commit();
                }
            }
        });
        return view;
    }

    private ArrayList<String> getHistory(SharedPreferences preferences) {
        String history = preferences.getString(HISTORY, "");
        return new ArrayList<String>(Arrays.asList(history.split(",")));
    }

    private String historyListToString(ArrayList<String> historyList) {
        String history = "";
        for (String entry : historyList) {
            history += entry + ",";
        }
        history = history.substring(0, history.length() - 1);

        return history;
    }

    private boolean wifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return mWifi.isConnected();
    }

    private boolean certificateIsImported() {
        File cert = new File(getContext().getFilesDir(), "burp.der");
        if (cert.exists() || checkBurpCert())
            return true;
        else
            return false;
    }

    public void proxySetting(boolean on) {
        if (on)
            MainActivity.executeCommandArgument(1, proxyAddress.getText().toString() + ":" + port.getText().toString());
        else
            MainActivity.executeCommand(1);
    }

    public boolean testConnection() {
        try {
            proxySetting(true);
            String url = "http://burp";
            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = new OkHttpClient();

            CallbackFuture future = new CallbackFuture();
            client.newCall(request).enqueue(future);
            Response response = future.get(2, TimeUnit.SECONDS); // To get async operation to sync operation

            if (response.isSuccessful()) {
                proxySetting(false);
                return true;
            }
            else {
                proxySetting(false);
                return false;
            }
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
            proxySetting(false);
            return false;
        }
    }

    public void installCertificate() {
        try {
            proxySetting(true);
            String url = "http://burp/cert";
            Request request = new Request.Builder().url(url).build();
            OkHttpClient client = new OkHttpClient();

            CallbackFuture future = new CallbackFuture();
            client.newCall(request).enqueue(future);
            Response response = future.get(); // To get async operation to sync operation

            if (response.isSuccessful()) {
                byte[] res = response.body().bytes();
                saveBurpDerFile(res);
                convertDerToPem();
                proxySetting(false);
                if (moveCertToUserCert()) {
                    MainActivity.executeCommand(5);
                } else
                    Toast.makeText(getContext(), "Error importing certificate!", Toast.LENGTH_SHORT).show();
            }
        }
        catch (IOException | InterruptedException | ExecutionException e) {

        }
    }

    private String convertToBase64(File file) throws IOException {

        InputStream inputStream = null;
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
        if (pem.endsWith("\n"))
            pem += bottom;
        else
            pem += "\n" + bottom;
        fos = getContext().openFileOutput("burp.pem", MODE_PRIVATE);
        fos.write(pem.getBytes());
    }

    private boolean moveCertToUserCert() {
        //9a5ba575.0
        if(MainActivity.executeCommandArgument( 2,getContext().getFilesDir() + ""))
            if(MainActivity.executeCommand(2))
                return true;
        return false;
    }

    private boolean checkBurpCert() {
        String output = MainActivity.executeCommandWithOutput();

        if(output.equals(""))
            return false;
        else
            return true;
    }
}
