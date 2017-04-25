package com.inuker.bluetooth.library;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleReadRssiResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.connect.response.BluetoothResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.receiver.BluetoothReceiver;
import com.inuker.bluetooth.library.receiver.listener.BleCharacterChangeListener;
import com.inuker.bluetooth.library.receiver.listener.BleConnectStatusChangeListener;
import com.inuker.bluetooth.library.receiver.listener.BluetoothBondListener;
import com.inuker.bluetooth.library.receiver.listener.BluetoothBondStateChangeListener;
import com.inuker.bluetooth.library.receiver.listener.BluetoothStateChangeListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;
import com.inuker.bluetooth.library.utils.ListUtils;
import com.inuker.bluetooth.library.utils.proxy.ProxyBulk;
import com.inuker.bluetooth.library.utils.proxy.ProxyInterceptor;
import com.inuker.bluetooth.library.utils.proxy.ProxyUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static com.inuker.bluetooth.library.Constants.CODE_CLEAR_REQUEST;
import static com.inuker.bluetooth.library.Constants.CODE_CONNECT;
import static com.inuker.bluetooth.library.Constants.CODE_DISCONNECT;
import static com.inuker.bluetooth.library.Constants.CODE_INDICATE;
import static com.inuker.bluetooth.library.Constants.CODE_NOTIFY;
import static com.inuker.bluetooth.library.Constants.CODE_READ;
import static com.inuker.bluetooth.library.Constants.CODE_READ_DESCRIPTOR;
import static com.inuker.bluetooth.library.Constants.CODE_READ_RSSI;
import static com.inuker.bluetooth.library.Constants.CODE_REFRESH_CACHE;
import static com.inuker.bluetooth.library.Constants.CODE_SEARCH;
import static com.inuker.bluetooth.library.Constants.CODE_STOP_SESARCH;
import static com.inuker.bluetooth.library.Constants.CODE_UNNOTIFY;
import static com.inuker.bluetooth.library.Constants.CODE_WRITE;
import static com.inuker.bluetooth.library.Constants.CODE_WRITE_DESCRIPTOR;
import static com.inuker.bluetooth.library.Constants.CODE_WRITE_NORSP;
import static com.inuker.bluetooth.library.Constants.DEVICE_FOUND;
import static com.inuker.bluetooth.library.Constants.EXTRA_BYTE_VALUE;
import static com.inuker.bluetooth.library.Constants.EXTRA_CHARACTER_UUID;
import static com.inuker.bluetooth.library.Constants.EXTRA_DESCRIPTOR_UUID;
import static com.inuker.bluetooth.library.Constants.EXTRA_GATT_PROFILE;
import static com.inuker.bluetooth.library.Constants.EXTRA_MAC;
import static com.inuker.bluetooth.library.Constants.EXTRA_OPTIONS;
import static com.inuker.bluetooth.library.Constants.EXTRA_REQUEST;
import static com.inuker.bluetooth.library.Constants.EXTRA_RSSI;
import static com.inuker.bluetooth.library.Constants.EXTRA_SEARCH_RESULT;
import static com.inuker.bluetooth.library.Constants.EXTRA_SERVICE_UUID;
import static com.inuker.bluetooth.library.Constants.EXTRA_TYPE;
import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.SEARCH_CANCEL;
import static com.inuker.bluetooth.library.Constants.SEARCH_START;
import static com.inuker.bluetooth.library.Constants.SEARCH_STOP;
import static com.inuker.bluetooth.library.Constants.SERVICE_UNREADY;

/**
 * Created by dingjikerbo on 16/4/8.
 */
public class BluetoothClientImpl implements IBluetoothClient, ProxyInterceptor, Callback {

    private static final int MSG_INVOKE_PROXY = 1;
    private static final int MSG_REG_RECEIVER = 2;

    private static final String TAG = BluetoothClientImpl.class.getSimpleName();

    private Context mContext;

    private volatile IBluetoothService mBluetoothService;

    private volatile static IBluetoothClient sInstance;

    private volatile CountDownLatch mCountDownLatch;

    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    private HashMap<String, HashMap<String, List<BleNotifyResponse>>> mNotifyResponses;
    private HashMap<String, List<BleConnectStatusListener>> mConnectStatusListeners;
    private List<BluetoothStateListener> mBluetoothStateListeners;
    private List<BluetoothBondListener> mBluetoothBondListeners;

    private BluetoothClientImpl(Context context) {
        mContext = context.getApplicationContext();
        BluetoothContext.set(mContext);

        mWorkerThread = new HandlerThread(TAG);
        mWorkerThread.start();

        mWorkerHandler = new Handler(mWorkerThread.getLooper(), this);

        mNotifyResponses = new HashMap<>();
        mConnectStatusListeners = new HashMap<>();
        mBluetoothStateListeners = new LinkedList<>();
        mBluetoothBondListeners = new LinkedList<>();

        mWorkerHandler.obtainMessage(MSG_REG_RECEIVER).sendToTarget();

//        BluetoothHooker.hook();
    }

    public static IBluetoothClient getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BluetoothClientImpl.class) {
                if (sInstance == null) {
                    BluetoothClientImpl client = new BluetoothClientImpl(context);
                    sInstance = ProxyUtils.getProxy(client, IBluetoothClient.class, client);
                }
            }
        }
        return sInstance;
    }

    @Override
    public void connect(String mac, BleConnectOptions options, final BleConnectResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect(String mac) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerConnectStatusListener(String mac, BleConnectStatusListener listener) {
        checkRuntime(true);
        List<BleConnectStatusListener> listeners = mConnectStatusListeners.get(mac);
        if (listeners == null) {
            listeners = new ArrayList<BleConnectStatusListener>();
            mConnectStatusListeners.put(mac, listeners);
        }
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterConnectStatusListener(String mac, BleConnectStatusListener listener) {
        checkRuntime(true);
        List<BleConnectStatusListener> listeners = mConnectStatusListeners.get(mac);
        if (listener != null && !ListUtils.isEmpty(listeners)) {
            listeners.remove(listener);
        }
    }

    @Override
    public void read(String mac, UUID service, UUID character, final BleReadResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(String mac, UUID service, UUID character, byte[] value, final BleWriteResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readDescriptor(String mac, UUID service, UUID character, UUID descriptor, final BleReadResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeDescriptor(String mac, UUID service, UUID character, UUID descriptor, byte[] value, final BleWriteResponse response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeNoRsp(String mac, UUID service, UUID character, byte[] value, final BleWriteResponse response) {
        throw new UnsupportedOperationException();
    }

    private void saveNotifyListener(String mac, UUID service, UUID character, BleNotifyResponse response) {
        checkRuntime(true);
        HashMap<String, List<BleNotifyResponse>> listenerMap = mNotifyResponses.get(mac);
        if (listenerMap == null) {
            listenerMap = new HashMap<String, List<BleNotifyResponse>>();
            mNotifyResponses.put(mac, listenerMap);
        }

        String key = generateCharacterKey(service, character);
        List<BleNotifyResponse> responses = listenerMap.get(key);
        if (responses == null) {
            responses = new ArrayList<BleNotifyResponse>();
            listenerMap.put(key, responses);
        }

        responses.add(response);
    }

    private void removeNotifyListener(String mac, UUID service, UUID character) {
        checkRuntime(true);
        HashMap<String, List<BleNotifyResponse>> listenerMap = mNotifyResponses.get(mac);
        if (listenerMap != null) {
            String key = generateCharacterKey(service, character);
            listenerMap.remove(key);
        }
    }

    private void clearNotifyListener(String mac) {
        checkRuntime(true);
        mNotifyResponses.remove(mac);
    }

    private String generateCharacterKey(UUID service, UUID character) {
        return String.format("%s_%s", service, character);
    }

    @Override
    public void notify(final String mac, final UUID service, final UUID character, final BleNotifyResponse response) {
        Bundle args = new Bundle();
        args.putString(EXTRA_MAC, mac);
        args.putSerializable(EXTRA_SERVICE_UUID, service);
        args.putSerializable(EXTRA_CHARACTER_UUID, character);
        safeCallBluetoothApi(CODE_NOTIFY, args, new BluetoothResponse() {
            @Override
            protected void onAsyncResponse(int code, Bundle data) {
                checkRuntime(true);
                if (response != null) {
                    if (code == REQUEST_SUCCESS) {
                        saveNotifyListener(mac, service, character, response);
                    }
                    response.onResponse(code);
                }
            }
        });
    }

    @Override
    public void unnotify(final String mac, final UUID service, final UUID character, final BleUnnotifyResponse response) {
        Bundle args = new Bundle();
        args.putString(EXTRA_MAC, mac);
        args.putSerializable(EXTRA_SERVICE_UUID, service);
        args.putSerializable(EXTRA_CHARACTER_UUID, character);
        safeCallBluetoothApi(CODE_UNNOTIFY, args, new BluetoothResponse() {
            @Override
            protected void onAsyncResponse(int code, Bundle data) {
                checkRuntime(true);

                removeNotifyListener(mac, service, character);

                if (response != null) {
                    response.onResponse(code);
                }
            }
        });
    }

    @Override
    public void indicate(final String mac, final UUID service, final UUID character, final BleNotifyResponse response) {
        Bundle args = new Bundle();
        args.putString(EXTRA_MAC, mac);
        args.putSerializable(EXTRA_SERVICE_UUID, service);
        args.putSerializable(EXTRA_CHARACTER_UUID, character);
        safeCallBluetoothApi(CODE_INDICATE, args, new BluetoothResponse() {
            @Override
            protected void onAsyncResponse(int code, Bundle data) {
                checkRuntime(true);
                if (response != null) {
                    if (code == REQUEST_SUCCESS) {
                        saveNotifyListener(mac, service, character, response);
                    }
                    response.onResponse(code);
                }
            }
        });
    }

    @Override
    public void unindicate(String mac, UUID service, UUID character, BleUnnotifyResponse response) {
       unnotify(mac, service, character, response);
    }

    @Override
    public void readRssi(String mac, final BleReadRssiResponse response) {
        Bundle args = new Bundle();
        args.putString(EXTRA_MAC, mac);
        safeCallBluetoothApi(CODE_READ_RSSI, args, new BluetoothResponse() {
            @Override
            protected void onAsyncResponse(int code, Bundle data) {
                checkRuntime(true);
                if (response != null) {
                    response.onResponse(code, data.getInt(EXTRA_RSSI, 0));
                }
            }
        });
    }

    @Override
    public void search(SearchRequest request, final SearchResponse response) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_REQUEST, request);
        safeCallBluetoothApi(CODE_SEARCH, args, new BluetoothResponse() {
            @Override
            protected void onAsyncResponse(int code, Bundle data) {
                checkRuntime(true);

                if (response == null) {
                    return;
                }

                data.setClassLoader(getClass().getClassLoader());

                switch (code) {
                    case SEARCH_START:
                        response.onSearchStarted();
                        break;

                    case SEARCH_CANCEL:
                        response.onSearchCanceled();
                        break;

                    case SEARCH_STOP:
                        response.onSearchStopped();
                        break;

                    case DEVICE_FOUND:
                        SearchResult device = data.getParcelable(EXTRA_SEARCH_RESULT);
                        response.onDeviceFounded(device);
                        break;

                    default:
                        throw new IllegalStateException("unknown code");
                }
            }
        });
    }

    @Override
    public void stopSearch() {
        safeCallBluetoothApi(CODE_STOP_SESARCH, null, null);
    }

    @Override
    public void registerBluetoothStateListener(BluetoothStateListener listener) {
        checkRuntime(true);
        if (listener != null && !mBluetoothStateListeners.contains(listener)) {
            mBluetoothStateListeners.add(listener);
        }
    }

    @Override
    public void unregisterBluetoothStateListener(BluetoothStateListener listener) {
        checkRuntime(true);
        if (listener != null) {
            mBluetoothStateListeners.remove(listener);
        }
    }

    @Override
    public void registerBluetoothBondListener(BluetoothBondListener listener) {
        checkRuntime(true);
        if (listener != null && !mBluetoothBondListeners.contains(listener)) {
            mBluetoothBondListeners.add(listener);
        }
    }

    @Override
    public void unregisterBluetoothBondListener(BluetoothBondListener listener) {
        checkRuntime(true);
        if (listener != null) {
            mBluetoothBondListeners.remove(listener);
        }
    }

    @Override
    public void clearRequest(String mac, int type) {
        Bundle args = new Bundle();
        args.putString(EXTRA_MAC, mac);
        args.putInt(EXTRA_TYPE, type);
        safeCallBluetoothApi(CODE_CLEAR_REQUEST, args, null);
    }

    @Override
    public void refreshCache(String mac) {
        checkRuntime(true);
        Bundle args = new Bundle();
        args.putString(EXTRA_MAC, mac);
        safeCallBluetoothApi(CODE_REFRESH_CACHE, args, null);
    }

    private IBluetoothService getBluetoothService() {
        checkRuntime(true);
        while (mBluetoothService == null) {
            bindServiceSync();
        }
        return mBluetoothService;
    }

    private void bindServiceSync() {
        checkRuntime(true);

        Intent intent = new Intent();
        intent.setClass(mContext, BluetoothService.class);

        if (mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            waitBluetoothManagerReady();
        } else {
            mBluetoothService = BluetoothServiceImpl.getInstance();
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = IBluetoothService.Stub.asInterface(service);
            notifyBluetoothManagerReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };

    @Override
    public boolean onIntercept(final Object object, final Method method, final Object[] args) {
        mWorkerHandler.obtainMessage(MSG_INVOKE_PROXY, new ProxyBulk(object, method, args))
                .sendToTarget();
        return true;
    }

    private void notifyBluetoothManagerReady() {
        if (mCountDownLatch == null) {
            throw new IllegalStateException();
        }
        mCountDownLatch.countDown();
        mCountDownLatch = null;
    }

    private void waitBluetoothManagerReady() {
        if (mCountDownLatch == null) {
            mCountDownLatch = new CountDownLatch(1);
        }
        try {
            mCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void safeCallRemote(Method method, Object[] args) {
        checkRuntime(true);

        IBluetoothService service = getBluetoothService();

        if (service != null) {
            try {
                Method remoteMethod = IBluetoothService.class.getDeclaredMethod(method.getName(), method.getParameterTypes());
                remoteMethod.setAccessible(true);
                remoteMethod.invoke(service, args);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_INVOKE_PROXY:
                ProxyBulk b = (ProxyBulk) msg.obj;
                safeCallRemote(b.method, b.args);

                break;
            case MSG_REG_RECEIVER:
                registerBluetoothReceiver();
                break;
        }
        return true;
    }

    private void registerBluetoothReceiver() {
        checkRuntime(true);
        BluetoothReceiver.getInstance().register(new BluetoothStateChangeListener() {
            @Override
            protected void onBluetoothStateChanged(int prevState, int curState) {
                checkRuntime(true);
                dispatchBluetoothStateChanged(curState);
            }
        });
        BluetoothReceiver.getInstance().register(new BluetoothBondStateChangeListener() {
            @Override
            protected void onBondStateChanged(String mac, int bondState) {
                checkRuntime(true);
                dispatchBondStateChanged(mac, bondState);
            }
        });
        BluetoothReceiver.getInstance().register(new BleConnectStatusChangeListener() {
            @Override
            protected void onConnectStatusChanged(String mac, int status) {
                checkRuntime(true);
                if (status == Constants.STATUS_DISCONNECTED) {
                    clearNotifyListener(mac);
                }
                dispatchConnectionStatus(mac, status);
            }
        });
        BluetoothReceiver.getInstance().register(new BleCharacterChangeListener() {
            @Override
            public void onCharacterChanged(String mac, UUID service, UUID character, byte[] value) {
                checkRuntime(true);
                dispatchCharacterNotify(mac, service, character, value);
            }
        });
    }

    private void dispatchCharacterNotify(String mac, UUID service, UUID character, byte[] value) {
        checkRuntime(true);
        HashMap<String, List<BleNotifyResponse>> notifyMap = mNotifyResponses.get(mac);
        if (notifyMap != null) {
            String key = generateCharacterKey(service, character);
            List<BleNotifyResponse> responses = notifyMap.get(key);
            if (responses != null) {
                for (final BleNotifyResponse response : responses) {
                    response.onNotify(service, character, value);
                }
            }
        }
    }

    private void dispatchConnectionStatus(final String mac, final int status) {
        checkRuntime(true);
        List<BleConnectStatusListener> listeners = mConnectStatusListeners.get(mac);
        if (!ListUtils.isEmpty(listeners)) {
            for (final BleConnectStatusListener listener : listeners) {
                listener.invokeSync(mac, status);
            }
        }
    }

    private void dispatchBluetoothStateChanged(final int currentState) {
        checkRuntime(true);
        if (currentState == Constants.STATE_OFF || currentState == Constants.STATE_ON) {
            for (final BluetoothStateListener listener : mBluetoothStateListeners) {
                listener.invokeSync(currentState == Constants.STATE_ON);
            }
        }
    }

    private void dispatchBondStateChanged(final String mac, final int bondState) {
        checkRuntime(true);
        for (final BluetoothBondListener listener : mBluetoothBondListeners) {
            listener.invokeSync(mac, bondState);
        }
    }

    private void checkRuntime(boolean async) {
        Looper targetLooper = async ? mWorkerHandler.getLooper() : Looper.getMainLooper();
        if (Looper.myLooper() != targetLooper) {
            throw new RuntimeException();
        }
    }
}
