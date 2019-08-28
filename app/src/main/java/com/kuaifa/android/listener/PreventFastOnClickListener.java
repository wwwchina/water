package com.kuaifa.android.listener;

import android.view.View;

/**
 *防止用户频繁点击
 */
public abstract class PreventFastOnClickListener implements View.OnClickListener{

	private long lastClickTime;
	private int mTimeInterval = 500;

	@Override
	public void onClick(View v) {
		if(!isFastClick()){
			OnClickLis(v);
		}
	}
	public synchronized boolean isFastClick() {
		long time = System.currentTimeMillis();
		long timeD = time - lastClickTime;   
		if (0 < timeD &&timeD< mTimeInterval) {
			return true;
		}
		lastClickTime = time;
		return false;
	}

	public abstract void OnClickLis(View v);
	
}
