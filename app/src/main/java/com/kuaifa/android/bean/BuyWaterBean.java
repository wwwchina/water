package com.kuaifa.android.bean;

//购水下单的bean
public class BuyWaterBean {

    /**
     * ResultCode : 1
     * ErrMsg :
     * info : {"payurl":"http://second.bczhiyinshui.com/attachments/qr/2019/201904201205332895.png","orderId":"DD2019042012053200017"}
     */

    private int ResultCode;
    private String ErrMsg;
    private InfoBean info;

    public int getResultCode() {
        return ResultCode;
    }

    public void setResultCode(int ResultCode) {
        this.ResultCode = ResultCode;
    }

    public String getErrMsg() {
        return ErrMsg;
    }

    public void setErrMsg(String ErrMsg) {
        this.ErrMsg = ErrMsg;
    }

    public InfoBean getInfo() {
        return info;
    }

    public void setInfo(InfoBean info) {
        this.info = info;
    }

    public static class InfoBean {
        /**
         * payurl : http://second.bczhiyinshui.com/attachments/qr/2019/201904201205332895.png
         * orderId : DD2019042012053200017
         */

        private String payurl;
        private String orderId;

        public String getPayurl() {
            return payurl;
        }

        public void setPayurl(String payurl) {
            this.payurl = payurl;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        @Override
        public String toString() {
            return "InfoBean{" +
                    "payurl='" + payurl + '\'' +
                    ", orderId='" + orderId + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BuyWaterBean{" +
                "ResultCode=" + ResultCode +
                ", ErrMsg='" + ErrMsg + '\'' +
                ", info=" + info +
                '}';
    }
}
