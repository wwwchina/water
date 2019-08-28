package com.kuaifa.android.bean;

//公司接水bean
public class CompanyWaterBean {

    /**
     * ResultCode : 1
     * ErrMsg :
     * info : {"qrurl":"","orderId":"DD2019042014372300049","watar_num":"10","orderstatus":1}
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
         * qrurl :
         * orderId : DD2019042014372300049
         * watar_num : 10
         * orderstatus : 1
         */

        private String qrurl;
        private String orderId;
        private String watar_num;
        private int orderstatus;

        public String getQrurl() {
            return qrurl;
        }

        public void setQrurl(String qrurl) {
            this.qrurl = qrurl;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }

        public String getWatar_num() {
            return watar_num;
        }

        public void setWatar_num(String watar_num) {
            this.watar_num = watar_num;
        }

        public int getOrderstatus() {
            return orderstatus;
        }

        public void setOrderstatus(int orderstatus) {
            this.orderstatus = orderstatus;
        }

        @Override
        public String toString() {
            return "InfoBean{" +
                    "qrurl='" + qrurl + '\'' +
                    ", orderId='" + orderId + '\'' +
                    ", watar_num='" + watar_num + '\'' +
                    ", orderstatus=" + orderstatus +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "CompanyWaterBean{" +
                "ResultCode=" + ResultCode +
                ", ErrMsg='" + ErrMsg + '\'' +
                ", info=" + info +
                '}';
    }
}
