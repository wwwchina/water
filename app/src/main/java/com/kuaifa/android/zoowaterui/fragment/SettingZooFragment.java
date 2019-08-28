package com.kuaifa.android.zoowaterui.fragment;


import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kuaifa.android.R;

/**
 * Zoo 设置页面
 */
public class SettingZooFragment extends Fragment implements View.OnClickListener{

    private View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_setting_zoo, container, false);
        initView();
        initData();
        return view;
    }

    private void initView() {
    }

    private void initData() {
    }

    @Override
    public void onClick(View v) {

    }
}
