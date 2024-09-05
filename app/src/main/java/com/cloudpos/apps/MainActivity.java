package com.cloudpos.apps;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.cloudpos.DeviceException;
import com.cloudpos.OperationResult;
import com.cloudpos.POSTerminal;
import com.cloudpos.TimeConstants;
import com.cloudpos.jniinterface.PinPadCallbackHandler;
import com.cloudpos.pinpad.KeyInfo;
import com.cloudpos.pinpad.PINPadDevice;
import com.cloudpos.pinpad.PINPadOperationResult;
import com.cloudpos.sdk.util.StringUtility;

import java.util.concurrent.ThreadPoolExecutor;


public class MainActivity extends Activity implements View.OnClickListener {
    private TextView message, log_text, log_text2;
    private Handler handler;
    private HandleCallBack callBack;
    private Button mStart;
    PINPadDevice device;

    public static final int PIN_KEY_CALLBACK = 4;
    private char[] stars = "********************************".toCharArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
    }

    private void initUI() {
        handler = new Handler(handleCallBack);
        callBack = new HandleCallbackImpl(this, handler);
        message = (TextView) findViewById(R.id.message);
        log_text = (TextView) findViewById(R.id.text_result);
        log_text2 = (TextView) findViewById(R.id.text_result2);
        mStart = (Button) findViewById(R.id.start);
        mStart.setOnClickListener(this);
        message.setText("");
        log_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        device = (PINPadDevice) POSTerminal.getInstance(this)
                .getDevice("cloudpos.device.pinpad");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.start) {
            log_text2.setText("Input password: \n");
            log_text.setText("");
            new ReadPINThread().start();
        }
    }

    private Handler.Callback handleCallBack = msg -> {
        switch (msg.what) {
            case HandleCallbackImpl.SUCCESS_CODE:
                setTextcolor(msg.obj.toString(), Color.BLUE);
                break;
            case HandleCallbackImpl.ERROR_CODE:
                setTextcolor(msg.obj.toString(), Color.RED);
                break;
            case PIN_KEY_CALLBACK:
                Log.e("PINPAD", "data[0] = " + msg.obj.toString());
                log_text2.setText("Password numbers: " + msg.obj.toString());
                if (Integer.parseInt(msg.obj.toString()) <= stars.length) {
                    log_text.setText(stars, 0, Integer.parseInt(msg.obj.toString()));
                }
                break;
            default:
                setTextcolor(msg.obj.toString(), Color.BLACK);
                break;
        }
        return false;
    };

    class ReadPINThread extends Thread {
        @Override
        public void run() {
            try {
                device.open();
                device.setupCallbackHandler(new PinPadCallbackHandler() {
                    @Override
                    public void processCallback(byte[] data) {
                        handler.obtainMessage(PIN_KEY_CALLBACK, data[0]).sendToTarget();
                    }

                    @Override
                    public void processCallback(int nCount, int nExtra) {
                        // don't need implement.

                    }
                });
                device.setGUIConfiguration("disablebackgrounddarkening", "true");
                KeyInfo keyInfo = new KeyInfo(PINPadDevice.KEY_TYPE_MK_SK, 0, 0, 4);
                String pan = "0123456789012345678";
                OperationResult operationResult = device.waitForPinBlock(keyInfo, pan, false,
                        TimeConstants.FOREVER);
                if (operationResult.getResultCode() == OperationResult.SUCCESS) {
                    byte[] pinBlock = ((PINPadOperationResult) operationResult).getEncryptedPINBlock();
                    callBack.sendResponse("PINBlock = " + StringUtility.byteArray2String(pinBlock));
                } else {
                    callBack.sendResponse("PINBlock number error! ");
                }
                device.close();
            } catch (DeviceException e) {
                e.printStackTrace();
                callBack.sendResponse("PINBlock failed!");
            }
        }
    }

    private void setTextcolor(String msg, int color) {
        Spannable span = Spannable.Factory.getInstance().newSpannable(msg);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
        span.setSpan(colorSpan, 0, span.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        message.append(span);
    }

}
