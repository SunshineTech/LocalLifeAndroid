package cn.sunshinebiz.locallife;

import android.os.Bundle;
import org.apache.cordova.*;

public class LocalLife extends DroidGap {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.loadUrl(Config.getStartUrl());
	}
}
