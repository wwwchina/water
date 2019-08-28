package com.kuaifa.android.ui.fragment;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kuaifa.android.MyApplication;
import com.kuaifa.android.R;
import com.kuaifa.android.bean.BuyWaterBean;
import com.kuaifa.android.bean.OrderStatusBean;
import com.kuaifa.android.ui.Constant;
import com.kuaifa.android.utils.ClickUtil;
import com.kuaifa.android.utils.DisplayImage;
import com.kuaifa.android.utils.MacUtil;
import com.kuaifa.android.utils.ToastUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.RequestParams;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest;

/**
 * 公司充值
 */
public class RechargeFragment extends BaseFragment implements View.OnClickListener {
    private RadioGroup rechargePayGroup;
    private String payType = "1";
    private TextView recharge_10;
    private TextView recharge_50;
    private TextView recharge_100;
    private TextView recharge_300;
    private TextView recharge_500;
    private TextView recharge_1000;
    private EditText rechargeInputCode;//接水码
    private EditText rechargeInputMoney;//充值金额
    private Button rechargeMakeSure;//确定按钮
    private LinearLayout recharge_window;
    private RadioButton recharge_alipay;
    private RadioButton recharge_wchat_pay;
    private String mac;
    private View rootView;

    @Override
    protected int getLayoutResource() {
        return R.layout.recharge_fragment;
    }

    @Override
    public void initView(View v) {
        rootView = v;
        rechargePayGroup = v.findViewById(R.id.rechargePayGroup);
        recharge_alipay = v.findViewById(R.id.recharge_alipay);
        recharge_wchat_pay = v.findViewById(R.id.recharge_wchat_pay);
        recharge_10 = v.findViewById(R.id.recharge_10);
        recharge_50 = v.findViewById(R.id.recharge_50);
        recharge_100 = v.findViewById(R.id.recharge_100);
        recharge_300 = v.findViewById(R.id.recharge_300);
        recharge_500 = v.findViewById(R.id.recharge_500);
        recharge_1000 = v.findViewById(R.id.recharge_1000);
        rechargeInputCode = v.findViewById(R.id.rechargeInputCode);
        rechargeInputMoney = v.findViewById(R.id.rechargeInputMoney);
        rechargeInputMoney.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rechargeMakeSure = v.findViewById(R.id.recharge_make_sure);
        recharge_window = v.findViewById(R.id.recharge_window);
        rechargeMakeSure.setOnClickListener(this);
        recharge_10.setOnClickListener(this);
        recharge_50.setOnClickListener(this);
        recharge_100.setOnClickListener(this);
        recharge_300.setOnClickListener(this);
        recharge_1000.setOnClickListener(this);
        recharge_500.setOnClickListener(this);

        rechargePayGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.recharge_wchat_pay:
                    payType = "1";
                    ToastUtils.showMessage(getActivity(),"微信");
                    recharge_alipay.setChecked(false);
                    recharge_wchat_pay.setChecked(true);
                    break;

                case R.id.recharge_alipay:
                    payType = "2";
                    ToastUtils.showMessage(getActivity(),"支付宝");
                    recharge_alipay.setChecked(true);
                    recharge_wchat_pay.setChecked(false);
                    break;

            }
        });

        initData();
        hideKeyboard(v);
    }


    public static void hideKeyboard(View view) {
        InputMethodManager imm = ( InputMethodManager ) view.getContext( ).getSystemService( Context.INPUT_METHOD_SERVICE );
        if ( imm.isActive( ) ) {
            imm.hideSoftInputFromWindow( view.getApplicationWindowToken( ) , 0 );
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.recharge_10:
                recharge_10.setSelected(true);
                recharge_50.setSelected(false);
                recharge_100.setSelected(false);
                recharge_300.setSelected(false);
                recharge_500.setSelected(false);
                recharge_1000.setSelected(false);
                rechargeInputMoney.setText("10");
                break;
            case R.id.recharge_50:
                recharge_10.setSelected(false);
                recharge_50.setSelected(true);
                recharge_100.setSelected(false);
                recharge_300.setSelected(false);
                recharge_500.setSelected(false);
                recharge_1000.setSelected(false);
                rechargeInputMoney.setText("50");
                break;
            case R.id.recharge_100:
                recharge_10.setSelected(false);
                recharge_50.setSelected(false);
                recharge_100.setSelected(true);
                recharge_300.setSelected(false);
                recharge_500.setSelected(false);
                recharge_1000.setSelected(false);
                rechargeInputMoney.setText("100");
                break;
            case R.id.recharge_300:
                recharge_10.setSelected(false);
                recharge_50.setSelected(false);
                recharge_100.setSelected(false);
                recharge_300.setSelected(true);
                recharge_500.setSelected(false);
                recharge_1000.setSelected(false);
                rechargeInputMoney.setText("300");
                break;
            case R.id.recharge_500:
                recharge_10.setSelected(false);
                recharge_50.setSelected(false);
                recharge_100.setSelected(false);
                recharge_300.setSelected(false);
                recharge_500.setSelected(true);
                recharge_1000.setSelected(false);
                rechargeInputMoney.setText("500");
                break;
            case R.id.recharge_1000:
                recharge_10.setSelected(false);
                recharge_50.setSelected(false);
                recharge_100.setSelected(false);
                recharge_300.setSelected(false);
                recharge_500.setSelected(false);
                recharge_1000.setSelected(true);
                rechargeInputMoney.setText("1000");
                break;
            case R.id.recharge_make_sure:
                if (ClickUtil.isFastClick()) {
                    hideKeyboard(rootView);
                    doClick();
                }
                break;
        }
    }


    public void initData(){
        mac = MacUtil.getMac(MyApplication.getInstance());
    }

    private void  doClick(){
        if(payType ==null){
            Toast.makeText(getActivity() ,"请选择支付类型",Toast.LENGTH_SHORT).show();
            return ;
        }
        if(rechargeInputCode.getText()==null || rechargeInputCode.getText().toString().equals("")){
            Toast.makeText(getActivity(), "请输入接水码",Toast.LENGTH_SHORT).show();
            return;
        }
        if(rechargeInputMoney.getText() == null || rechargeInputMoney.getText().toString().equals("")){
            Toast.makeText(getActivity(), "请选择或输入支付金额",Toast.LENGTH_SHORT).show();
            return;
        }
        String jieWaterCode = rechargeInputCode.getText()+"";
        String money = rechargeInputMoney.getText()+"";
//        mPresenter.buyWaterRequest(payType, jieWaterCode ,money);
        //充值接口
        companyRecharge(payType,jieWaterCode,money);
    }


    public void returnBuyWater(String json) {
        Log.d("--------------recharge-",json);
        if (json != null) {
            if (json.contains("[]")){
                json = json.replace("[]","{}");
            }
            BuyWaterBean buyWaterBean = new Gson().fromJson(json,BuyWaterBean.class);
            BuyWaterBean.InfoBean infoBean = buyWaterBean.getInfo();
            if (infoBean != null && !infoBean.toString().equals("{}")){
                if (TextUtils.isEmpty(infoBean.getPayurl()) || TextUtils.isEmpty(infoBean.getOrderId())){
                    return;
                }
                String QRUrl = infoBean.getPayurl().startsWith("http://") ? infoBean.getPayurl() : Constant.ORDERURL+infoBean.getPayurl();
                showPopupWindow(QRUrl, infoBean.getOrderId(), recharge_window);
                recharge_10.setSelected(false);
                recharge_50.setSelected(false);
                recharge_100.setSelected(false);
                recharge_300.setSelected(false);
                recharge_500.setSelected(false);
                recharge_1000.setSelected(false);
                rechargeInputCode.setText("");
                rechargeInputMoney.setText("");

            }else {
                ToastUtils.showMessage(MyApplication.getInstance(),buyWaterBean.getErrMsg());
            }
        }
    }


    //显示popupWindow
    private void showPopupWindow(String url , final String orderId , View view){
        View popupWindow_view = getLayoutInflater().inflate(R.layout.item_popupwindow, null,false);
        final PopupWindow popupWindow =  new PopupWindow(popupWindow_view,500 , ViewGroup.LayoutParams.WRAP_CONTENT, true);
        backgroundAlpha(0.4f);
        ImageView m2 = popupWindow_view.findViewById(R.id.item_two);
        TextView m3 = popupWindow_view.findViewById(R.id.item_three);
        DisplayImage.displayImage(url, m2);
        m3.setOnClickListener(view1 -> {
            backgroundAlpha(1.0f);
            popupWindow.dismiss();
            checkPayStatus(orderId);//查询付款状态
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

    //获取支付成功、失败的状态
    public void returnOrderStatus(String json) {
        Log.d("-----------充值状态",json);
        if(!TextUtils.isEmpty(json)){
            OrderStatusBean orderStatusBean = new Gson().fromJson(json,OrderStatusBean.class);
            if (orderStatusBean.getResultCode()==1) {
                OrderStatusBean.InfoBean infoBean = orderStatusBean.getInfo();
                if (infoBean.getOrderstatus()==1) {  //支付成功的
                    Toast.makeText(getActivity(), "充值成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "支付失败，如有疑问请致电客服", Toast.LENGTH_SHORT).show();
                }
            }else {
                ToastUtils.showMessage(MyApplication.getInstance(),orderStatusBean.getErrMsg());
            }
        }
    }

    //公司充值接口
    public void companyRecharge(String payType, String watercode, final String recharge_num){
        RequestParams params = new RequestParams();
        params.addBodyParameter("pay_type",payType);
        params.addBodyParameter("watercode",watercode);
        params.addBodyParameter("recharge_num",recharge_num);
        params.addBodyParameter("equipment_no", mac);

        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "site/recharge.html", params, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                returnBuyWater(responseInfo.result);
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

    //查看付款状态
    public void checkPayStatus(final String orderId){
        RequestParams params = new RequestParams();
        params.addBodyParameter("orderId",orderId);
        params.addBodyParameter("equipment_no", mac);
        HttpUtils utils = new HttpUtils();
        utils.send(HttpRequest.HttpMethod.POST, Constant.ORDERURL + "site/orderstatus.html", params, new RequestCallBack<String>() {
            @Override
            public void onSuccess(ResponseInfo<String> responseInfo) {
                //{"ResultCode":1,"ErrMsg":"","info":{"payurl":"http://second.bczhiyinshui.com/attachments/qr/2019/201904201457108019.png","orderId":"DD2019042014571000055"}}
                returnOrderStatus(responseInfo.result);
            }

            @Override
            public void onFailure(HttpException e, String s) {
                Log.d("----------公司充值",s);
            }

            @Override
            public void onLoading(long total, long current, boolean isUploading) {
                super.onLoading(total, current, isUploading);
            }
        });
    }
}
