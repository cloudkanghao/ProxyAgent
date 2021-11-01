package com.kh.proxyagent.Fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kh.proxyagent.R;

import org.jetbrains.annotations.NotNull;

public class SettingFragment extends Fragment {

    private View view;
    private EditText proxyAddress, port;
    private Button saveButton;

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting, container, false);
        proxyAddress = view.findViewById(R.id.proxyAddress);
        port = view.findViewById(R.id.port);
        saveButton = view.findViewById(R.id.settingSubmit);

        SharedPreferences preferences = this.getActivity().getPreferences(Context.MODE_PRIVATE);

        String proxyAddressValue = preferences.getString("proxyAddress", "");
        String portValue = preferences.getString("port", "");
        proxyAddress.setText(proxyAddressValue);
        port.setText(portValue);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Toast.makeText(getContext(), "trying hard to save...", Toast.LENGTH_SHORT).show();

                // 1: FIX POWER ON CLICKING SETTING BUGS
                // 2: CHECK IF CONNECTED TO WIFI FIRST
                String proxyAddressValue = proxyAddress.getText().toString(), portValue = port.getText().toString();
                boolean validPort = true;
                try {
                    int intValue = Integer.parseInt(portValue);
                } catch (NumberFormatException e) {
                    validPort = false;
                }

                if(Patterns.IP_ADDRESS.matcher(proxyAddressValue).matches() && validPort) {
                    SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
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

                    Toast.makeText(getContext(), "Setting updated!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getContext(), "invalid IP or port", Toast.LENGTH_SHORT).show();
                }

            }
        });
        return view;
    }
}
