package com.example.haoji.userActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.haoji.Button.ButtonData;
import com.example.haoji.HttpUtil;
import com.example.haoji.R;
import com.example.haoji.dailyActivity.dailyActivity;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import cn.smssdk.EventHandler;
 import cn.smssdk.SMSSDK;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    private Button getCode;
    private Button modifyPsw;
    private EditText userphone,edit_code,password;
    private int i = 60;//����ʱ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        userphone = (EditText) findViewById(R.id.userphone);
        edit_code = (EditText) findViewById(R.id.edit_code);
        password = (EditText) findViewById(R.id.password);
        getCode = (Button) findViewById(R.id.getCode);
        modifyPsw = (Button) findViewById(R.id.modifyPsw);
        getCode.setOnClickListener(this);
        modifyPsw.setOnClickListener(this);
        init();
    }
    private void init(){
        EventHandler eventHandler = new EventHandler(){
            @Override
            public void afterEvent(int event, int result, Object data) {
                Message msg = new Message();
                msg.arg1 = event;
                msg.arg2 = result;
                msg.obj = data;
                handler.sendMessage(msg);
            }
        };
        //ע��ص������ӿ�
        SMSSDK.registerEventHandler(eventHandler);
    }
    @Override
    public void onClick(View v) {
        String phoneNum = userphone.getText().toString();
       // String passWord = password.getText().toString();
        switch (v.getId()) {
            case R.id.getCode:
                // 1. ͨ�������ж��ֻ���
                if (!judgephoneNum(phoneNum)) {
                    return;
                } // 2. ͨ��sdk�����ȡ������֤��
                SMSSDK.getVerificationCode("86", phoneNum);

                // 3. �Ѱ�ť��ɲ��ɵ����������ʾ����ʱ�����ڻ�ȡ��
                getCode.setClickable(false);
                getCode.setText("���·���(" + i + ")");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (; i > 0; i--) {
                            handler.sendEmptyMessage(-9);
                            if (i <= 0) {
                                break;
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        handler.sendEmptyMessage(-8);
                    }
                }).start();
                break;

            case R.id.modifyPsw:
                //�ύ������֤�룬�ڼ����з���
                SMSSDK.submitVerificationCode("86", phoneNum, edit_code
                        .getText().toString());
                break;
        }
    }

         Handler handler = new Handler() {
             public void handleMessage(Message msg) {
                 String phoneNum = userphone.getText().toString();
                 String passWord = password.getText().toString();
                 if (msg.what == -9) {
                     getCode.setText("���·���(" + i + ")");
                 } else if (msg.what == -8) {
                     getCode.setText("��ȡ��֤��");
                     getCode.setClickable(true);
                     i = 60;
                 } else {
                     int event = msg.arg1;
                     int result = msg.arg2;
                     Object data = msg.obj;
                     Log.e("event", "event=" + event);
                     if (result == SMSSDK.RESULT_COMPLETE) {
                         //�ص����
                         if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
                             // �ύ��֤��ɹ����ύע��
                             Toast.makeText(getApplicationContext(), "�ύ��֤��ɹ�",
                                     Toast.LENGTH_SHORT).show();
                             if (isValid())
                             //����JSONObject���͵�����
                             {
                                 JSONObject jsonObject = new JSONObject();
                                 try {
                                     jsonObject.put("username",phoneNum);
                                     jsonObject.put("password",passWord);
                                     jsonObject.put("phonenum",phoneNum);
                                     jsonObject.put("qq"," ");
                                     jsonObject.put("weibo"," ");
                                 } catch (JSONException e) {
                                     e.printStackTrace();
                                 }
                                 //��������
                                 HttpUtil.sendOkHttpRequest("http://97.64.21.155:8001/user/signup ", jsonObject, new okhttp3.Callback() {
                                     public void onResponse(Call call, Response response) throws IOException {
                                         String responseData = response.body().string();
                                         if(judgeState(responseData)){
                                             //showres(responseData);
                                             Intent intent = new Intent(RegisterActivity.this,
                                                     dailyActivity.class);
                                             startActivity(intent);
                                             //finish();
                                         }
                                         else{
                                             errorTip();
                                         }
                                     }
                                     public void onFailure(Call call, IOException e) {
                                         e.printStackTrace();
                                     }
                                 });

                             }

                         } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                             //��ȡ��֤��ɹ�
                             Toast.makeText(getApplicationContext(), "��֤���Ѿ�����",
                                     Toast.LENGTH_SHORT).show();
                         } else {
                             ((Throwable) data).printStackTrace();
                         }
                     }
                     if (result == SMSSDK.RESULT_ERROR) {
                         Toast.makeText(getApplicationContext(), "��֤�����",
                                 Toast.LENGTH_SHORT).show();
                     }
                 }
             }
         };


    /**
     * �ж��ֻ������Ƿ����
     *
     * @param phoneNum
     */
    private boolean judgephoneNum(String phoneNum) {
        if (isMatchLength(phoneNum, 11)
                && isMobileNO(phoneNum)) {
            return true;
        }
        Toast.makeText(this, "�ֻ�������������",Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * �ж�һ���ַ�����λ��
     * @param str
     * @param length
     * @return
     */
    public static boolean isMatchLength(String str, int length) {
        if (str.isEmpty()) {
            return false;
        } else {
            return str.length() == length ? true : false;
        }
    }
public boolean isValid(){
        if (password.getText().toString().trim().equals("")) {
        Toast.makeText(this,"���벻��Ϊ��",Toast.LENGTH_SHORT).show();
        return false;
        }
        return true;
        }
    private boolean judgeState(String responseData) {
        try{
            JSONObject jsonObject = new JSONObject(responseData);
            int state = jsonObject.getInt("state");
            if(state==200) return true;
            else return false;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ��֤�ֻ���ʽ
     */
    public static boolean isMobileNO(String mobileNums) {
		/*
		 * �ƶ���134��135��136��137��138��139��150��151��157(TD)��158��159��187��188
		 * ��ͨ��130��131��132��152��155��156��185��186 ���ţ�133��153��180��189����1349��ͨ��
		 * �ܽ��������ǵ�һλ�ض�Ϊ1���ڶ�λ�ض�Ϊ3��5��8������λ�õĿ���Ϊ0-9
		 */
        String telRegex = "[1][358]\\d{9}";// "[1]"�����1λΪ����1��"[358]"����ڶ�λ����Ϊ3��5��8�е�һ����"\\d{9}"��������ǿ�����0��9�����֣���9λ��
        if (TextUtils.isEmpty(mobileNums))
            return false;
        else
            return mobileNums.matches(telRegex);
    }

    @Override
    protected void onDestroy() {
        SMSSDK.unregisterAllEventHandler();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.menu_register, menu);
        return true;
    }
//    private void showres (final String s){
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(RegisterActivity.this,s,Toast.LENGTH_LONG ).show() ;
//            }
//        }) ;
//    }
    private void errorTip (){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RegisterActivity.this,"ע��ʧ��",Toast.LENGTH_LONG ).show() ;
            }
        }) ;
    }


    public boolean onOptionsitemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
