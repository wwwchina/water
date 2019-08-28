package com.kuaifa.android.bean;

//检查支付状态的Bean
public class OrderStatusBean {

    /**
     * ResultCode : 1
     * ErrMsg :
     * info : {"orderstatus":2,"pay_purpose":1,"watar_num":7}
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
         * orderstatus : 2    1：成功    2：失败
         * pay_purpose : 1    1：购水    2：充值
         * watar_num : 7
         */

        private int orderstatus;
        private int pay_purpose;
        private double watar_num;

        public int getOrderstatus() {
            return orderstatus;
        }

        public void setOrderstatus(int orderstatus) {
            this.orderstatus = orderstatus;
        }

        public int getPay_purpose() {
            return pay_purpose;
        }

        public void setPay_purpose(int pay_purpose) {
            this.pay_purpose = pay_purpose;
        }

        public double getWatar_num() {
            return watar_num;
        }

        public void setWatar_num(double watar_num) {
            this.watar_num = watar_num;
        }

        @Override
        public String toString() {
            return "InfoBean{" +
                    "orderstatus=" + orderstatus +
                    ", pay_purpose=" + pay_purpose +
                    ", watar_num=" + watar_num +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OrderStatusBean{" +
                "ResultCode=" + ResultCode +
                ", ErrMsg='" + ErrMsg + '\'' +
                ", info=" + info +
                '}';
    }
}
