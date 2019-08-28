package com.kuaifa.android.ui.fragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.kuaifa.android.MyApplication;
import com.kuaifa.android.R;
import com.kuaifa.android.bean.CompanyWaterBean;
import com.kuaifa.android.bean.OrderStatusBean;
import com.kuaifa.android.bean.QRbean;
import com.kuaifa.android.serialport.SerialPortFinder;
import com.kuaifa.android.ui.Constant;
import com.kuaifa.android.ui.activity.DialogCommCountActivity;
import com.kuaifa.android.ui.activity.MainActivity;
import com.kuaifa.android.ui.manager.SerialPortManager;
import com.kuaifa.android.utils.ClickUtil;
import com.kuaifa.android.utils.MacUtil;
import com.kuaifa.android.utils.ToastUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

import java.util.logging.LogRecord;

/**
 * 员工接水
 */
public class StaffFragment extends BaseFragment implements View.OnClickListener{

    private TextView staff_7;
    private TextView staff_15;
    private TextView staff_18;
    private EditText staff_input_num;//输入取水量
    private EditText staff_input_code;//输入接水码
    private Button makeSure;//确定
    private String mac;
    private View rootView;
    private AlertDialog.Builder builder;
    private TextView staffTdsValue;

    @Override
    protected int getLayoutResource() {
        return R.layout.staff_fragment;
    }

    @Override
    public void initView(View v){
        SerialPortManager.instance().sendCommand("FDFDFD");
        rootView = v;
        staff_7 = v.findViewById(R.id.staff_7);
        staff_15 = v.findViewById(R.id.staff_15);
        staff_18 = v.findViewById(R.id.staff_18);
        staff_input_num = v.findViewById(R.id.staff_input_num);
        staff_input_num.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        makeSure = v.findViewById(R.id.staff_make_sure);
        staff_input_code = v.findViewById(R.id.staff_input_code);
        staff_7.setOnClickListener(this);
        staff_15.setOnClickListener(this);
        staff_18.setOnClickListener(this);
        makeSure.setOnClickListener(this);
        staff_7.setSelected(true);
        staffTdsValue = v.findViewById(R.id.staff_tds_value);
        hideKeyboard(v);
        initData();
    }

    private void initData() {
        mac = MacUtil.getMac(MyApplication.getInstance());

       refresh();
    }

    public static void hideKeyboard(View v) {
        InputMethodManager imm = ( InputMethodManager ) v.getContext( ).getSystemService( Context.INPUT_METHOD_SERVICE );
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow( v.getApplicationWindowToken( ) , 0 );
        }
    }

    public void refresh() {
        if (staffTdsValue != null){
            MainActivity.fixedThreadPool.execute(() -> {
                SerialPortManager.instance().sendCommand("FDFDFD");
                SystemClock.sleep(000);
                Log.e(">>>>>>>>>>>>水质=",SerialPortManager.instance().getShuizhi()+"");

                getActivity().runOnUiThread(() -> staffTdsValue.setText("TDS水质:" + SerialPortManager.instance().getShuizhi()));
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.staff_7:
                staff_7.setSelected(true);
                staff_15.setSelected(false);
                staff_18.setSelected(false);
                staff_input_num.setText(7.0 + "");
                break;
            case R.id.staff_15:
                staff_7.setSelected(false);
                staff_15.setSelected(true);
                staff_18.setSelected(false);
                staff_input_num.setText(15 + "");
                break;
            case R.id.staff_18:
                staff_7.setSelected(false);
                staff_15.setSelected(false);
                staff_18.setSelected(true);
                staff_input_num.setText(18 + "");
                break;
            case R.id.staff_make_sure://点击确定开始接水
                if (ClickUtil.isFastClick()) {
                    hideKeyboard(rootView);
                    doClick();
                }
                break;
        }
    }

    private void doClick(){

        if (staff_input_num.getText() == null || (staff_input_num.getText().toString()).equals("")) {
            Toast.makeText(getActivity(), "请选择取水量", Toast.LENGTH_SHORT).show();
            return;
        }

        String jieWaterCode = staff_input_code.getText()+"";
        String waterNumber = staff_input_num.getText()+"";
        requestWater(jieWaterCode,waterNumber);

    }

    //公司接水解析json
    public void returnJieWater(String json) {
        if (TextUtils.isEmpty(json)){
            return;
        }
        if (json.contains("[]")){
            json = json.replace("[]","{}");
        }
        CompanyWaterBean companyWaterBean = null;
        try {
            companyWaterBean = new Gson().fromJson(json,CompanyWaterBean.class);
        }catch (Exception e){
            ToastUtils.showMessage(getActivity(),e.getMessage());
        }
        if (companyWaterBean==null){
            return;
        }
        CompanyWaterBean.InfoBean infoBean = companyWaterBean.getInfo();
        if (infoBean != null && !infoBean.toString().equals("{}")){
            if(infoBean.getOrderstatus() == 1 ){
                Intent intent = new Intent(getActivity() , DialogCommCountActivity.class);
                intent.putExtra("waterNumber" , infoBean.getWatar_num());
                getActivity().startActivity(intent);

            }else{
                Toast.makeText(getActivity() , "支付失败，请检查接水码是否正确，且资金是否充足",Toast.LENGTH_LONG).show();
                return;
            }
        }else {
            ToastUtils.showMessage(MyApplication.getInstance(),companyWaterBean.getErrMsg());
        }
    }

    //接水请求接口
    public void requestWater(String waterCode ,String waterNumber){
        RequestParams params = new RequestParams();
        params.addBodyParameter("watercode",waterCode);
        params.addBodyParameter("watar_num",waterNumber);
//        params.addBodyParameter("type","1");//添加此参数生成URL
        params.addBodyParameter("equipment_no", mac);
        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "site/jiewater.html", params, new RequestCallBack<String>() {

            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                if (TextUtils.isEmpty(waterCode)) {//为空，展示二维码
                    try {
                        QRbean qRbean = new Gson().fromJson(responseInfo.result,QRbean.class);
                        showDialog(qRbean);
                    }catch (Exception e){
                        e.getStackTrace();
                    }

                }else {
                    returnJieWater(responseInfo.result);
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                ToastUtils.showMessage(MyApplication.getInstance(),s);
            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }

            @Override
            public void onStart() {
                super.onStart();
                staff_input_code.setText("");
            }
        });
    }


    //输入接水码点击确定，弹出二维码
    private void showDialog(QRbean bean) {
        if (TextUtils.isEmpty(bean.getInfo().getQrurl())){
            ToastUtils.showMessage(MyApplication.getInstance(),"二维码为空！");
            return;
        }
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(MyApplication.getInstance()).inflate(R.layout.layout_qr, null);
        ImageView staffQr = view.findViewById(R.id.staff_qr);
        builder = new AlertDialog.Builder(getActivity()).setView(view)
                .setTitle("扫描二维码，绑定个人信息")
                .setIcon(R.mipmap.ic_launcher)
                .setPositiveButton("确定", (dialog, which) -> {
                    // 点击验证订单是否支付
                    requestOderStatus(bean.getInfo().getOrderId(),mac);

                });
        Glide.with(getContext()).load(Constant.ORDERURL+bean.getInfo().getQrurl()).into(staffQr);
        builder.create().show();
    }

    //请求接口，获取订单状态
    public void requestOderStatus(String orderId,String mac){

        RequestParams params = new RequestParams();
        params.addBodyParameter("orderId",orderId);
        params.addBodyParameter("equipment_no", mac);
        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "sitenew/orderstatus.html", params, new RequestCallBack<String>() {

            @Override
            public void onStart() {
                super.onStart();
//                staff_input_num.setText("");
            }

            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                if (TextUtils.isEmpty(orderId)) {
                    return;
                }
                try {
                    OrderStatusBean bean = new Gson().fromJson(responseInfo.result,OrderStatusBean.class);
                    if (bean != null){
                        if (bean.getInfo().getOrderstatus()==1){//支付成功跳转
                            Intent intent = new Intent(getActivity() , DialogCommCountActivity.class);
                            intent.putExtra("waterNumber" , String.valueOf(bean.getInfo().getWatar_num()));
                            getActivity().startActivity(intent);
                        }else {
                            ToastUtils.showMessage(MyApplication.getInstance(),"您还没有扫码，订单失败！");
                        }
                    }
                }catch (Exception e){
                    e.getStackTrace();
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                ToastUtils.showMessage(MyApplication.getInstance(),s);
            }

        });
    }
}
