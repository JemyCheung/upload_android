package com.qiniu.jemy.upload.activity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.android.http.custom.DnsCacheKey;
import com.qiniu.android.storage.Recorder;
import com.qiniu.android.storage.persistent.DnsCacheFile;
import com.qiniu.android.utils.AndroidNetwork;
import com.qiniu.android.utils.LogHandler;
import com.qiniu.jemy.upload.R;
import com.qiniu.jemy.upload.utils.Config;
import com.qiniu.jemy.upload.utils.FileUtils;
import com.qiniu.jemy.upload.utils.TempFile;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.AsyncRun;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mStartBtn, mClearLog, mOpenFile;

    private static final int REQUEST_CODE = 8090;
    private TextView mLog;
    private EditText keyNameEdit, tokenEdit, fileSizeEdit;
    String[] allpermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private String token;
    private UploadManager uploadManager;
    private String keyname;
    private long uploadFileLength;
    private String uploadFilePath;
    private RadioGroup radioGroup;
    private RadioButton radioButton1, radioButton2, radioButton3;
    private File uploadFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.qiniu.jemy.upload.R.layout.activity_main);
        applypermission();
        initView();
        initUploadManager();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case com.qiniu.jemy.upload.R.id.openFile:
                Intent target = FileUtils.createGetContentIntent();
                Intent intent = Intent.createChooser(target,
                        this.getString(com.qiniu.jemy.upload.R.string.choose_file));
                try {
                    this.startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException ex) {
                }
                break;

            case com.qiniu.jemy.upload.R.id.start:
                upload();
                break;
            case R.id.clear_log:
                clearLog();
                break;
        }
    }


    private void initView() {
        mOpenFile = findViewById(com.qiniu.jemy.upload.R.id.openFile);
        mStartBtn = findViewById(com.qiniu.jemy.upload.R.id.start);
        mClearLog = findViewById(R.id.clear_log);
        keyNameEdit = findViewById(com.qiniu.jemy.upload.R.id.keyname);
        tokenEdit = findViewById(com.qiniu.jemy.upload.R.id.uptoken);
        fileSizeEdit = findViewById(R.id.filesize);
        radioGroup = findViewById(R.id.region);
        radioButton1 = findViewById(R.id.region_z0);
        radioButton2 = findViewById(R.id.region_z1);
        radioButton3 = findViewById(R.id.region_z2);
        mLog = findViewById(com.qiniu.jemy.upload.R.id.log);

        mLog.setMovementMethod(ScrollingMovementMethod.getInstance());
        mOpenFile.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
        mClearLog.setOnClickListener(this);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                setRegion(checkedId);
            }
        });
    }

    private void initUploadManager() {
        if (this.uploadManager == null) {
            Configuration configuration = new Configuration.Builder()
                    .useHttps(true)
                    .logHandler(new LogHandler() {
                        @Override
                        public void send(String msg) {
                            writeLog(msg);
                        }
                    }).build();
            this.uploadManager = new UploadManager(configuration, 3);
            writeLog("初始化 UploadManager");
        }
    }

    private void setRegion(int checkedId) {
        switch (checkedId) {
            case R.id.region_z0:
                token = Config.UPTOKEN_Z0;
                break;
            case R.id.region_z1:
                token = Config.UPTOKEN_Z1;
                break;
            case R.id.region_z2:
                token = Config.UPTOKEN_Z2;
                break;
        }
    }


    private void upload() {

        if (!checkValue()) {
            return;
        }

        final long startTime = System.currentTimeMillis();
        HashMap<String, String> map = new HashMap<>();
        map.put("netCheckTime", "10");
        UploadOptions opt = new UploadOptions(map, null, true, new UpProgressHandler() {
            @Override
            public void progress(String key, double percent) {
                Log.i("qiniutest", "percent:" + percent);
            }
        }, null, new LogHandler() {
            @Override
            public void send(String msg) {
                writeLog(msg);
            }
        });

        //获取本地dns缓存记录
        if (!output()) {
            writeLog("获取 IP 发生错误");
        }

        this.uploadManager.put(uploadFile, keyname, token,
                new UpCompletionHandler() {
                    @Override
                    public void complete(String key, ResponseInfo respInfo,
                                         JSONObject jsonData) {
                        long endTime = System.currentTimeMillis();
                        if (respInfo.isOK()) {
                            try {
                                Log.e("zw", jsonData.toString() + respInfo.toString());
                                writeLog("UPTime/ms: " + (endTime - startTime));
                                String fileKey = jsonData.getString("key");
                                String fileHash = jsonData.getString("hash");
//                                writeLog("File Size: " + Tools.formatSize(uploadFileLength));
//                                writeLog("File Key: " + fileKey);
                                writeLog("File Hash: " + fileHash + "\nkey: " + fileKey);
                                writeLog("X-Reqid: " + respInfo.reqId);
                                writeLog("host: " + respInfo.host);
                                // writeLog("X-Via: " + respInfo.xvia);
                                writeLog("--------------------------------上传成功\n\n");
                            } catch (JSONException e) {
                                writeLog(MainActivity.this
                                        .getString(com.qiniu.jemy.upload.R.string.qiniu_upload_file_response_parse_error));
                                if (jsonData != null) {
                                    writeLog(jsonData.toString());
                                }
                                writeLog("--------------------------------上传失败\n\n");
                            }
                        } else {
                            writeLog(respInfo.toString());
                            if (jsonData != null) {
                                writeLog(jsonData.toString());
                            }
                            writeLog("--------------------------------上传失败\n\n");
                        }
                    }

                }, opt);
    }

    private boolean checkValue() {
        String uptoken = tokenEdit.getText().toString().trim();
        String filename = keyNameEdit.getText().toString().trim();
        String filesize = fileSizeEdit.getText().toString().trim();

        if (!"".equals(uptoken) && uptoken != null) {
            token = uptoken;
        } else if (token == null) {
            Toast.makeText(this, "请输入token或者勾选上传区域", Toast.LENGTH_LONG).show();
            return false;
        }

        keyname = "android_test_" + filename + new Date().getTime();

        if (!"".equals(filesize) && filesize != null) {
            try {
                uploadFile = TempFile.createFile((int) Long.parseLong(filesize) * 1024);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (uploadFilePath == null) {
            Toast.makeText(this, "请选择文件或者输入文件大小自动生成", Toast.LENGTH_LONG).show();
            return false;
        } else {
            uploadFile = new File(uploadFilePath);
        }
        uploadFileLength = uploadFile.length();
        return true;
    }

    private boolean output() {
        String ip = AndroidNetwork.getHostIP();
        Recorder recorder = null;
        try {
            recorder = new DnsCacheFile(com.qiniu.android.collect.Config.dnscacheDir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        String dnscache = recorder.getFileName();
        if (dnscache == null)
            return false;

        byte[] data = recorder.get(dnscache);
        if (data == null)
            return false;

        DnsCacheKey cacheKey = DnsCacheKey.toCacheKey(dnscache);
        if (cacheKey == null)
            return false;

        String cacheIp = cacheKey.getLocalIp();
        if (cacheIp == null)
            return false;
        writeLog("--------------------------------开始上传\n本机ip:" + ip + ",上次缓存ip:" + cacheIp);
        return true;
    }

    public void applypermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean needapply = false;
            for (int i = 0; i < allpermissions.length; i++) {
                int chechpermission = ContextCompat.checkSelfPermission(getApplicationContext(),
                        allpermissions[i]);
                if (chechpermission != PackageManager.PERMISSION_GRANTED) {
                    needapply = true;
                }
            }
            if (needapply) {
                ActivityCompat.requestPermissions(MainActivity.this, allpermissions, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, permissions[i] + "已授权", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, permissions[i] + "拒绝授权", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(this, uri);
                            this.uploadFilePath = path;
                            this.writeLog(this
                                    .getString(com.qiniu.jemy.upload.R.string.qiniu_select_upload_file)
                                    + path);
                        } catch (Exception e) {
                            Toast.makeText(
                                    this,
                                    this.getString(com.qiniu.jemy.upload.R.string.qiniu_get_upload_file_failed),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void clearLog() {
        this.mLog.setText("");
    }

    public void writeLog(final String msg) {
        AsyncRun.runInMain(new Runnable() {
            @Override
            public void run() {
                mLog.append(msg);
                mLog.append("\r\n");
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
