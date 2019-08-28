package com.kuaifa.android.bean;

public class QRbean {

    /**
     * info : {"qrurl":"upload/code/c86afbfc-f31f-3e82-853d-a6b15ec54b0a.png","orderId":"DD2019081413001500035","watar_num":"7.0","orderstatus":2}
     */

    private InfoBean info;

    public InfoBean getInfo() {
        return info;
    }

    public void setInfo(InfoBean info) {
        this.info = info;
    }

    public static class InfoBean {
        /**
         * qrurl : upload/code/c86afbfc-f31f-3e82-853d-a6b15ec54b0a.png
         * orderId : DD2019081413001500035
         * watar_num : 7.0
         * orderstatus : 2
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
    }
}
