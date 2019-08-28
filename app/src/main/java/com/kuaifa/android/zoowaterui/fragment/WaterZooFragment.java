package com.kuaifa.android.zoowaterui.fragment;


import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.kuaifa.android.MyApplication;
import com.kuaifa.android.R;
import com.kuaifa.android.bean.BuyWaterBean;
import com.kuaifa.android.bean.OrderStatusBean;
import com.kuaifa.android.manager.VideoPlayManager;
import com.kuaifa.android.ui.Constant;
import com.kuaifa.android.utils.MacUtil;
import com.kuaifa.android.utils.ToastUtils;
import com.kuaifa.android.zoowaterui.activity.DialogZooCountActivity;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

public class WaterZooFragment extends Fragment implements View.OnClickListener{

    private View view;
    private int type= 1;//1冷水  2，热水
    private ImageView alipay;
    private ImageView wchatpay;
    private TextView num100;
    private TextView num200;
    private TextView num500;
    private TextView makeSure;
    private ImageView start;
    private EditText input_water_num;//输入接水量
    private ImageView QR_code;
    private String payType = null;//支付类型
    private String mac= "";
    private TextView tdsShow;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_water_zoo, container, false);
        initView();
        initData();
        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        input_water_num.setText(null);

        //tds先设置随机数
        int start=8;
        int end=12;
        int random = (int) (Math.random()*(end-start)+start);
        tdsShow.setText(Html.fromHtml("<font color=\'#ebe6c0\'>TDS水质检测</font><font color=\'#f48421\'> "+random +"</font><font color=\'#ebe6c0\'>可放心饮用</font>"));
    }

    public void initView(){
        alipay = view.findViewById(R.id.alipay);
        wchatpay = view.findViewById(R.id.wchatpay);
        num100 = view.findViewById(R.id.num100);
        num200 = view.findViewById(R.id.num200);
        num500 = view.findViewById(R.id.num500);
        makeSure = view.findViewById(R.id.makeSure);
        start = view.findViewById(R.id.start);
        input_water_num = view.findViewById(R.id.input_water_num);
        QR_code = view.findViewById(R.id.QR_code);
        tdsShow = view.findViewById(R.id.tdsshow);
        String html = Html.fromHtml("<font color=\'#ebe6c0\'>TDS水质检测</font><font color=\'#f48421\'> 10 </font><font color=\'#ebe6c0\'>可放心饮用</font>")+"";
        tdsShow.setText(html);
        alipay.setOnClickListener(this);
        wchatpay.setOnClickListener(this);
        num100.setOnClickListener(this);
        num200.setOnClickListener(this);
        num500.setOnClickListener(this);
        makeSure.setOnClickListener(this);
        start.setOnClickListener(this);

    }

    public void initData(){
        mac = MacUtil.getMac(MyApplication.getInstance());
    }


    public void setType(int type){
        this.type = type;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.alipay:
                payType = "2";
                alipay.setImageResource(R.mipmap.zfb);
                wchatpay.setImageResource(R.mipmap.img_wchatpay);
                break;
            case R.id.wchatpay:
                payType = "1";
                alipay.setImageResource(R.mipmap.img_alipay);
                wchatpay.setImageResource(R.mipmap.wx);
                break;
            case R.id.num100:
                input_water_num.setText("100");
                break;
            case R.id.num200:
                input_water_num.setText("200");
                break;
            case R.id.num500:
                input_water_num.setText("500");
                break;
            case R.id.makeSure://下单，获取二维码
                doClick();
                break;
            case R.id.start://开始接水
                payType = null;
                alipay.setImageResource(R.mipmap.img_alipay);
                wchatpay.setImageResource(R.mipmap.img_wchatpay);
                input_water_num.setText("");
                Glide.with(MyApplication.getInstance()).load("").into(QR_code);
/////////////////////////////////////////////////////测试////////////////////////////////////////////////////////////
//                测试用，直接跳转接水页面
//                Intent intent = new Intent(getActivity(), DialogZooCountActivity.class);
//                intent.putExtra("waterNumber","1" );
//                intent.putExtra("type", type);
//                getActivity().startActivityForResult(intent,1231);
/////////////////////////////////////////////////////正式///////////////////////////////////////////////////////////////
                checkPayStatus(orderId);
/////////////////////////////////////////////////////正式//////////////////////////////////////////////////////////////
                break;
        }
    }

    //点击获取订单号和支付二维码
    private void doClick() {
        if (TextUtils.isEmpty(payType)) {
            Toast.makeText(MyApplication.getInstance(), "请选择支付类型", Toast.LENGTH_SHORT).show();
            return;
        }
        if (input_water_num.getText() == null || (input_water_num.getText().toString()).equals("")) {
            Toast.makeText(MyApplication.getInstance(), "请选择取水量", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(mac)){
            ToastUtils.showMessage(MyApplication.getInstance(),"mac为空");
            return;
        }
        String waterNumber = input_water_num.getText() + "";
        double num = 0;
        try {
            num = Double.parseDouble(waterNumber);
            if (num<100){
                ToastUtils.showMessage(MyApplication.getInstance(),"最小接水量是100ml");
                return;
            }
        }catch (Exception e){
            ToastUtils.showMessage(MyApplication.getInstance(),e.getMessage());
            return;
        }
        String waternum = String.valueOf(num/1000);
        requestOrder(payType, waternum);//下单接口
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
                returnBuyWater(responseInfo.result);

            }

            @Override
            public void onFailure(HttpException e, String s) {
                ToastUtils.showMessage(MyApplication.getInstance(),s);
            }
        });
    }

    //获取订单号成功后显示二维码
    public void returnBuyWater(String json) {
        Log.d("-------------person-",json);
        if (TextUtils.isEmpty(json)){
            return;
        }
        BuyWaterBean buyWaterBean = null;
        try {
            buyWaterBean = new Gson().fromJson(json, BuyWaterBean.class);
        }catch (Exception e){
            ToastUtils.showMessage(MyApplication.getInstance(),e.getMessage());
        }

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
                Glide.with(MyApplication.getInstance()).load(QRUrl).into(QR_code);
                orderId = infoBean.getOrderId();
            }
        }else {
            ToastUtils.showMessage(MyApplication.getInstance(),buyWaterBean.getErrMsg());
        }
    }

    //查看付款状态
    private String orderId = null;
    public void checkPayStatus(final String orderId) {
        if (TextUtils.isEmpty(orderId)){
            ToastUtils.showMessage(MyApplication.getInstance(),"订单号为空！");
            return;
        }
        RequestParams params = new RequestParams();
        params.addBodyParameter("orderId", orderId);
        params.addBodyParameter("equipment_no", mac);
        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "site/orderstatus.html", params, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                try {
                    returnOrderStatus(responseInfo.result);
                }catch (Exception e){
                    ToastUtils.showMessage(MyApplication.getInstance(),"订单异常");
                    VideoPlayManager.setTenSecond();
                }
            }

            @Override
            public void onFailure(HttpException e, String s) {
                ToastUtils.showMessage(MyApplication.getInstance(),s);
                VideoPlayManager.setTenSecond();
            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }
        });
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
                    orderId = null;
                    Intent intent = new Intent(getActivity(), DialogZooCountActivity.class);
                    intent.putExtra("waterNumber", infoBean.getWatar_num()+"");
                    Log.d("----------","---------------------infoBean.getWatar_num="+infoBean.getWatar_num());
                    intent.putExtra("type", type);
//                    getActivity().startActivity(intent);
                    VideoPlayManager.stop();
                    getActivity().startActivityForResult(intent,1231);
                } else {
                    Toast.makeText(MyApplication.getInstance(), "支付失败，如有疑问请致电客服", Toast.LENGTH_SHORT).show();
                    VideoPlayManager.setTenSecond();
                }
            }

        }else {
            ToastUtils.showMessage(MyApplication.getInstance(),orderStatusBean.getErrMsg());
        }
    }
}
