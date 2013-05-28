package cn.sunshinebiz.cordova.plugin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

public class BaiduGeoBroker extends CordovaPlugin {

	private static String TAG = "BaiduGeoBroker";
	private LocationClient locationClient = null;
	private LocationListenner locationListenner = null;
	private HashMap<String, NotifyListener> watches = new HashMap<String, NotifyListener>();
	
	public BaiduGeoBroker() {}
	
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		
		if (locationClient == null) {
			locationClient = new LocationClient(this.cordova.getActivity().getApplicationContext());
			locationListenner = new LocationListenner();
			locationClient.registerLocationListener(locationListenner);
			locationClient.start();
		}
		
		Log.d(TAG, "Location request: " + action);
		
		if (action.equals("getLocation")) {
            boolean enableHighAccuracy = args.optBoolean(0);
            int maximumAge = args.optInt(1);
            
            BDLocation last = locationClient.getLastKnownLocation();
			boolean flag = false;
			long t = 0;
			if (last != null) {
				flag = (enableHighAccuracy && last.getLocType() == BDLocation.TypeGpsLocation)
						|| (!enableHighAccuracy && (last.getLocType() == BDLocation.TypeCacheLocation
								|| last.getLocType() == BDLocation.TypeOffLineLocation
								|| last.getLocType() == BDLocation.TypeOffLineLocationNetworkFail 
								|| last.getLocType() == BDLocation.TypeNetWorkLocation));
				try {
					t = System.currentTimeMillis() - (new SimpleDateFormat("yy-MM-dd hh:mm:ss", Locale.CHINA).parse(last.getTime()).getTime());
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			
			if (last != null && flag & t <= maximumAge) {
				Log.i(TAG, "Get the last known location");
				PluginResult result = new PluginResult(PluginResult.Status.OK, this.returnLocationJSON(last));
				callbackContext.sendPluginResult(result);
			} else {
				getCurrentLocation(args, callbackContext);
			}
		} else if (action.equals("addWatch")) {
            addWatch(args, callbackContext);
        } else if (action.equals("clearWatch")) {
        	clearWatch(args, callbackContext);
        } else {
        	return false;
        }
		
		return true;
	}

	public JSONObject returnLocationJSON(BDLocation loc) {
		
		JSONObject o = new JSONObject();
		try {
			o.put("latitude", loc.getLatitude());
			o.put("longitude", loc.getLongitude());
			o.put("altitude", loc.hasAltitude() ? loc.getAltitude() : null);
			o.put("accuracy", loc.hasRadius() ? loc.getRadius() : null);
			o.put("heading", loc.getLocType() == BDLocation.TypeGpsLocation ? loc.getDerect() : null);
			o.put("speed", (loc.hasSpeed() ? loc.getSpeed() : null));
			long timestamp = 0;
			try {
				timestamp = (new SimpleDateFormat("yy-MM-dd hh:mm:ss", Locale.CHINA).parse(loc.getTime()).getTime());
			} catch (ParseException e) {
				Log.e(TAG, e.getMessage());
			}
			o.put("timestamp", timestamp);
			o.put("locType", loc.getLocType());
			o.put("stateNum", loc.hasSateNumber() ? loc.getSatelliteNumber() : null);
			o.put("addr", loc.hasAddr() ? loc.getAddrStr() : null);
			o.put("city", loc.hasAddr() ? loc.getCity() : null);
			o.put("cityCode", loc.hasAddr() ? loc.getCityCode() : null);
			o.put("coorType", loc.getCoorType());
			o.put("district", loc.hasAddr() ? loc.getDistrict() : null);
			o.put("province", loc.hasAddr() ? loc.getProvince() : null);
			o.put("street", loc.hasAddr() ? loc.getStreet() : null);
			o.put("streetNo", loc.hasAddr() ? loc.getStreetNumber() : null);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}

		return o;
	}

	private void getCurrentLocation(JSONArray args, CallbackContext callbackContext) {
		Log.d(TAG, "Method: getCurrentLocation");
		LocationClientOption option = new LocationClientOption();
		option.disableCache(args.optBoolean(2));
		option.setAddrType(args.optString(3, "none"));
		option.setCoorType(args.optString(4, "bd09ll"));
		//option.setLocationNotify(args.optBoolean(4));
		option.setOpenGps(args.optBoolean(0));
		//option.setPoiDistance((float)args.optDouble(5, 500));
		//option.setPoiExtraInfo(args.optBoolean(6));
		//option.setPoiNumber(args.optInt(7));
		option.setPriority(args.optBoolean(0) ? LocationClientOption.GpsFirst : LocationClientOption.NetWorkFirst);
		option.setProdName(args.optString(8, "Cordova plugin of Baidu Location of SunshineTech Ltd,co"));
		//option.setScanSpan(arg0)
		//option.setServiceName(arg0)
		//option.setTimeOut(arg0)
		
		locationClient.setLocOption(option);
		locationListenner.addCallback(callbackContext);		
	}
	
	private void addWatch(JSONArray args, CallbackContext callbackContext) {
		BDLocation last = locationClient.getLastKnownLocation();
		if(last != null) {
			Log.d(TAG, "Method: addWatch");
			NotifyListener notifyListener = new NotifyListener(callbackContext);
			this.watches.put(args.optString(0), notifyListener);
			notifyListener.SetNotifyLocation(last.getLatitude(), last.getLongitude(), 10, "bd09ll");
			LocationClientOption option = new LocationClientOption();
			option.setOpenGps(args.optBoolean(1));
			option.setPriority(args.optBoolean(1) ? LocationClientOption.GpsFirst : LocationClientOption.NetWorkFirst);
			locationClient.setLocOption(option);
			locationClient.registerNotify(notifyListener);
		}		
	}
	
	private void clearWatch(JSONArray args, CallbackContext callbackContext) {
		NotifyListener notifyListener = this.watches.remove(args.optString(0));
		if(notifyListener != null) {
			locationClient.removeNotifyEvent(notifyListener);
			LocationClientOption option = new LocationClientOption();
			option.setOpenGps(false);
			locationClient.setLocOption(option);
		}
	}
	
	public void fail(int code, String msg, CallbackContext callbackContext) {
		
        JSONObject obj = new JSONObject();
        String backup = null;
        try {
            obj.put("code", code);
            obj.put("message", msg);
        } catch (JSONException e) {
            obj = null;
            backup = "{'code':" + code + ",'message':'" + msg.replaceAll("'", "\'") + "'}";
        }
        
        PluginResult result;
        if (obj != null) {
            result = new PluginResult(PluginResult.Status.ERROR, obj);
        } else {
            result = new PluginResult(PluginResult.Status.ERROR, backup);
        }

        callbackContext.sendPluginResult(result);
    }
	
	public void win(BDLocation loc, CallbackContext callbackContext) {
		Log.d(TAG, "Get location success");
        PluginResult result = new PluginResult(PluginResult.Status.OK, this.returnLocationJSON(loc));
        callbackContext.sendPluginResult(result);
        Log.d(TAG, "call back finished");
    }
	
	public void onDestroy() {
		Log.i(TAG, "onDestroy");
        if (this.locationClient != null) {
        	if(this.locationListenner != null) {
        		locationClient.unRegisterLocationListener(locationListenner);
        		locationListenner = null;
        	}
        	
        	Iterator<NotifyListener> it = this.watches.values().iterator();
            while (it.hasNext()) {
            	NotifyListener notifyListener = it.next();
            	locationClient.removeNotifyEvent(notifyListener);
            	notifyListener = null;
            }
            this.watches.clear();
            this.watches = null;
        	
        	if (this.locationClient.isStarted()) {
        		locationClient.stop();
        		locationClient = null;
        	}
        }
    }
	
	public void onReset() {
		Log.i(TAG, "onDestroy");
        this.onDestroy();
    }

	private class LocationListenner implements BDLocationListener {
		
		private List<CallbackContext> callbacks = new ArrayList<CallbackContext>();
		
		private static final int POSITION_UNAVAILABLE = 2;
		
		public int size() {
	        return this.callbacks.size();
	    }
		
		public void addCallback(CallbackContext callbackContext) {
			Log.d(TAG, "add callback");
	        this.callbacks.add(callbackContext);
	        if (this.size() == 1) {
	        	locationClient.requestLocation();
	        }
	    }

		public void onReceiveLocation(BDLocation location) {
			Log.i(TAG, "Receive location");
			if (location == null) {
				this.fail(POSITION_UNAVAILABLE, "Provider is out of service.");
				Log.e(TAG, "Location is null");
			} else
				switch(location.getLocType()) {
				case BDLocation.TypeGpsLocation:
				case BDLocation.TypeCacheLocation:
				case BDLocation.TypeNetWorkLocation:
					this.win(location);
					break;
				case BDLocation.TypeCriteriaException:
				case BDLocation.TypeNetWorkException:
					this.fail(location.getLocType(), "Can not locate!");
					Log.i(TAG, "Can not locate!");
					break;
				case 162-167:
					this.fail(location.getLocType(), "Server Error");
					Log.e(TAG, "Server Error!");
					break;
				default: 
					this.fail(location.getLocType(), "Other Error");
					Log.i(TAG, "Other Error");
				}
		}

		public void onReceivePoi(BDLocation arg0) {
			// TODO Auto-generated method stub
			
		}
		
		private void stop() {
			LocationClientOption option = new LocationClientOption();
			option.setOpenGps(false);
			locationClient.setLocOption(option);
		}
		
		private void fail(int code, String message) {
			
	        for (CallbackContext callbackContext: this.callbacks) {
	        	BaiduGeoBroker.this.fail(code, message, callbackContext);
	        }
	        this.stop();
	        this.callbacks.clear();
	    }
		
		private void win(BDLocation loc) {
			
	        for (CallbackContext callbackContext: this.callbacks) {
	        	Log.i(TAG, "CallbackId: " + callbackContext.getCallbackId());
	        	BaiduGeoBroker.this.win(loc, callbackContext);
	        }
	        this.stop();
	        this.callbacks.clear();
	    }		
	}
	
	private class NotifyListener extends BDNotifyListener {
		
		CallbackContext callbackContext = null;

		public NotifyListener(CallbackContext callbackContext) {
			this.callbackContext = callbackContext;
		}

		@Override
		public void onNotify(BDLocation location, float distance) {
			
			this.SetNotifyLocation(location.getLatitude(), location.getLongitude(), 10, "bd09ll");
			
			PluginResult result = new PluginResult(PluginResult.Status.OK, BaiduGeoBroker.this.returnLocationJSON(location));
			result.setKeepCallback(true);
	        callbackContext.sendPluginResult(result);
		}
	}
}
