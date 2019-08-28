package com.kuaifa.android.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.kuaifa.android.MyApplication;
import com.kuaifa.android.R;
import com.kuaifa.android.bean.BuyWaterBean;
import com.kuaifa.android.bean.OrderStatusBean;
import com.kuaifa.android.serialport.SerialPort;
import com.kuaifa.android.ui.Constant;
import com.kuaifa.android.ui.activity.DialogCommCountActivity;
import com.kuaifa.android.ui.activity.MainActivity;
import com.kuaifa.android.ui.manager.SerialPortManager;
import com.kuaifa.android.utils.ClickUtil;
import com.kuaifa.android.utils.DisplayImage;
import com.kuaifa.android.utils.MacUtil;
import com.kuaifa.android.utils.ToastUtils;
import com.kuaifa.android.utils.ZXingUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

/**
 * 个人购水
 */
public class PersonalFragment extends BaseFragment implements View.OnClickListener {

    private LinearLayout person_window;
    private RadioGroup personPayGroup;
    private TextView person_7;
    private TextView person_15;
    private TextView person_18;
    private EditText personInputNum;//输入框
    private Button makeSure;//确定按钮
    private String payType = "1";
//    private String mac;
    private View rootView;
    private  TextView personTdsValue;//TDS值
    @Override
    protected int getLayoutResource() {
        return R.layout.person_fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void initView(View v) {
        rootView = v;
        person_window = v.findViewById(R.id.person_window);
        personPayGroup = v.findViewById(R.id.personPayGroup);
        person_7 = v.findViewById(R.id.person_7);
        person_15 = v.findViewById(R.id.person_15);
        person_18 = v.findViewById(R.id.person_18);
        personInputNum = v.findViewById(R.id.person_input_num);
        makeSure = v.findViewById(R.id.person_makesure);
        person_7.setOnClickListener(this);
        person_15.setOnClickListener(this);
        person_18.setOnClickListener(this);
        makeSure.setOnClickListener(this);
        personTdsValue = v.findViewById(R.id.tds_value);

        initData();
        hideKeyboard(v);

    }

    private void initData() {
//        mac = MacUtil.getMac(MyApplication.getInstance());
        personPayGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.person_alipay:
                    payType = "2";
                    ToastUtils.showMessage(getActivity(), "支付宝");
                    break;
                case R.id.person_wchatpay:
                    payType = "1";
                    ToastUtils.showMessage(getActivity(), "微信");
                    break;
            }
        });

      refresh();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.person_7:
                person_7.setSelected(true);
                person_15.setSelected(false);
                person_18.setSelected(false);
                personInputNum.setText("7.0");
                break;
            case R.id.person_15:
                person_7.setSelected(false);
                person_15.setSelected(true);
                person_18.setSelected(false);
                personInputNum.setText("15");
                break;
            case R.id.person_18:
                person_7.setSelected(false);
                person_15.setSelected(false);
                person_18.setSelected(true);
                personInputNum.setText("18");
                break;
            case R.id.person_makesure:
                if (ClickUtil.isFastClick()) {
                    hideKeyboard(rootView);
                    doClick();
                }
                break;
        }
    }

    public void refresh(){
        if (personTdsValue!= null) {
            MainActivity.fixedThreadPool.execute(() -> {
                SerialPortManager.instance().sendCommand("FDFDFD");
                SystemClock.sleep(1000);
                Log.e(">>>>>>>>>>>>水质=",SerialPortManager.instance().getShuizhi()+"");
                getActivity().runOnUiThread(() ->
                        personTdsValue.setText("TDS水质:" + SerialPortManager.instance().getShuizhi()));
            });


        }
    }

    //点击获取订单号和支付二维码
    private void doClick() {
        if (payType == null) {
            Toast.makeText(getActivity(), "请选择支付类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (personInputNum.getText() == null || (personInputNum.getText().toString()).equals("")) {
            Toast.makeText(getActivity(), "请选择取水量", Toast.LENGTH_SHORT).show();
            return;
        }

        String waterNumber = personInputNum.getText() + "";
        if (payType == "1")
            payType = "3";
        else if (payType == "2")
            payType = "4";
        requestOrder(payType, waterNumber);//下单接口
    }

    public static void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        }
    }

    //获取订单号成功后显示二维码
    public void returnBuyWater(String json) {
        if (TextUtils.isEmpty(json)){
            return;
        }
        BuyWaterBean buyWaterBean = new Gson().fromJson(json,BuyWaterBean.class);
        if (buyWaterBean ==null){
            return;
        }
        if (buyWaterBean.getResultCode()==1){
            BuyWaterBean.InfoBean infoBean = buyWaterBean.getInfo();
            if (infoBean!= null){
                if (TextUtils.isEmpty(infoBean.getPayurl()) || TextUtils.isEmpty(infoBean.getOrderId())){
                    return;
                }
                String QRUrl = infoBean.getPayurl().startsWith("http://") ? infoBean.getPayurl() : Constant.ORDERURL + infoBean.getPayurl();
                showPopupWindow(QRUrl, infoBean.getOrderId(), person_window);
                personInputNum.setText("");
            }
        }else {
            ToastUtils.showMessage(MyApplication.getInstance(),buyWaterBean.getErrMsg());
        }
    }

    //支付成功或失败的状态
    public void returnOrderStatus(String json) {
        if (TextUtils.isEmpty(json)){
            return;
        }
        OrderStatusBean orderStatusBean = new Gson().fromJson(json,OrderStatusBean.class);
        if (orderStatusBean.getResultCode()==1){
            OrderStatusBean.InfoBean infoBean = orderStatusBean.getInfo();
            if (infoBean != null){
                if (infoBean.getOrderstatus() == 1) {  //支付成功
                    if (infoBean.getPay_purpose()==1){
                        ToastUtils.showMessage(MyApplication.getInstance(),"购水成功");
                    }else if(infoBean.getPay_purpose()==2){
                        ToastUtils.showMessage(MyApplication.getInstance(),"充值成功");
                    }
                    Intent intent = new Intent(getActivity(), DialogCommCountActivity.class);
                    intent.putExtra("waterNumber", infoBean.getWatar_num()+"");
                    getActivity().startActivity(intent);
                } else {
                    Toast.makeText(getActivity(), "支付失败，如有疑问请致电客服", Toast.LENGTH_SHORT).show();
                }
            }

        }else {
            ToastUtils.showMessage(MyApplication.getInstance(),orderStatusBean.getErrMsg());
        }
    }

    //显示二维码
    private void showPopupWindow(String url, final String orderId, View view) {
        View popupWindow_view = getLayoutInflater().inflate(R.layout.item_popupwindow, null);
        final PopupWindow popupWindow = new PopupWindow(popupWindow_view, 500, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        backgroundAlpha(0.4f);
        ImageView m2 = popupWindow_view.findViewById(R.id.item_two);
        TextView m3 = popupWindow_view.findViewById(R.id.item_three);
        DisplayImage.displayImage(url, m2);
        m3.setOnClickListener(view1 -> {
            backgroundAlpha(1.0f);
            popupWindow.dismiss();

            //查看付款状态
            checkPayStatus(orderId);
        });
        popupWindow.setAnimationStyle(R.style.popup_window_anim);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    public void backgroundAlpha(float bgalpha) {
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = bgalpha;
        getActivity().getWindow().setAttributes(lp);
    }

    //请求接口获取二维码和订单号
    public void requestOrder(final String payType, String waterNum) {
        RequestParams params = new RequestParams();
        params.addBodyParameter("pay_type", payType);
        params.addBodyParameter("watar_num", waterNum);
//        params.addBodyParameter("equipment_no", mac);
        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "site/buywater.html", params, new RequestCallBack<String>() {

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }

            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                // returnBuyWater(responseInfo.result);
                returnBuyWater2(responseInfo.result);
            }

            @Override
            public void onFailure(HttpException e, String s) {
                ToastUtils.showMessage(MyApplication.getInstance(),s);
            }
        });
    }

    // doogi 20190813
    public void returnBuyWater2(String json) {
        if (TextUtils.isEmpty(json)){
            return;
        }
        BuyWaterBean buyWaterBean = new Gson().fromJson(json,BuyWaterBean.class);
        if (buyWaterBean ==null){
            return;
        }
        if (buyWaterBean.getResultCode()==1){
            BuyWaterBean.InfoBean infoBean = buyWaterBean.getInfo();
            if (infoBean!= null){
                if (TextUtils.isEmpty(infoBean.getPayurl()) || TextUtils.isEmpty(infoBean.getOrderId())){
                    return;
                }
                //String QRUrl = infoBean.getPayurl().startsWith("http://") ? infoBean.getPayurl() : Constant.ORDERURL + infoBean.getPayurl();
                String QRUrl =infoBean.getPayurl();
                showPopupWindow2(QRUrl, infoBean.getOrderId(), person_window);
                personInputNum.setText("");
            }
        }else {
            ToastUtils.showMessage(MyApplication.getInstance(),buyWaterBean.getErrMsg());
        }
    }

    private void showPopupWindow2(String url, final String orderId, View view) {
        View popupWindow_view = getLayoutInflater().inflate(R.layout.item_popupwindow, null);
        final PopupWindow popupWindow = new PopupWindow(popupWindow_view, 500, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        backgroundAlpha(0.4f);
        ImageView m2 = popupWindow_view.findViewById(R.id.item_two);
        TextView m3 = popupWindow_view.findViewById(R.id.item_three);

        // doogi 20190813
        //DisplayImage.displayImage(url, m2);
        Bitmap bm = ZXingUtils.createQRImage(url,528,528);
        m2.setImageBitmap(bm);
        m3.setOnClickListener(view1 -> {
            backgroundAlpha(1.0f);
            popupWindow.dismiss();

            //查看付款状态
            checkPayStatus(orderId);
        });
        popupWindow.setAnimationStyle(R.style.popup_window_anim);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    //查看付款状态
    public void checkPayStatus(final String orderId) {
        RequestParams params = new RequestParams();
        params.addBodyParameter("orderId", orderId);
//        params.addBodyParameter("equipment_no", mac);
        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "site/orderstatus.html", params, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                returnOrderStatus(responseInfo.result);
            }

            @Override
            public void onFailure(HttpException e, String s) {
                ToastUtils.showMessage(MyApplication.getInstance(),s);
            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }
        });
    }
}

