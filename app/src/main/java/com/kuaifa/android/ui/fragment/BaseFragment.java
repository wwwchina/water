package com.kuaifa.android.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;


/**
 * Created by lbc on 2017/3/29.
 */

public abstract class BaseFragment extends Fragment  {
    protected View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(getLayoutResource(), container, false);
        }
        getLayoutResource();
        initView(rootView);
//        hideKeyboard(rootView);
        return rootView;
    }

    //隐藏和显示键盘
    public static void hideKeyboard( View view) {
        InputMethodManager imm = ( InputMethodManager ) view.getContext( ).getSystemService( Context.INPUT_METHOD_SERVICE );
        if ( imm.isActive( ) ) {
            imm.hideSoftInputFromWindow( view.getApplicationWindowToken( ) , 0 );
        }
    }

    //获取布局文件v
    protected abstract int getLayoutResource();

    //初始化view
    protected abstract void initView(View v);

}
