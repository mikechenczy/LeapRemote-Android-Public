package org.mj.leapremote.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.Notification;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mask.mediaprojection.utils.MediaProjectionHelper;

import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.ui.activities.ControlActivity;
import org.mj.leapremote.Define;
import org.mj.leapremote.LogUtil;
import org.mj.leapremote.NotificationHelper;
import org.mj.leapremote.R;
import org.mj.leapremote.cs.direct.NettyClientDirect;
import org.mj.leapremote.receiver.DeviceAdmin;
import org.mj.leapremote.service.ServerService;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.DiscoverNetIpUtil;
import org.mj.leapremote.util.NetWorkUtil;
import org.mj.leapremote.util.Utils;

import java.util.List;

public class QuickConnectFragment extends Fragment {
    public EditText hostText;
    public EditText portText;
    public Button connectDirect;
    public EditText connectIdText;
    public EditText connectPinText;
    public Button connect;
    public Button changePin;
    public Button copyIp;
    //DevicePolicyManager deviceManger;
    ComponentName compName;
    boolean isBound;
    public ServerService service;
    public View view;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            isBound = true;
            ServerService.MyBinder myBinder = (ServerService.MyBinder)binder;
            service = myBinder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = inflater.inflate(R.layout.fragment_quick_connect, container, false);
        initData();
        hostText = view.findViewById(R.id.host_text);
        portText = view.findViewById(R.id.port_text);
        connectDirect = view.findViewById(R.id.connect_direct);
        connectIdText = view.findViewById(R.id.connect_id_text);
        connectPinText = view.findViewById(R.id.connect_pin_text);
        connect = view.findViewById(R.id.connect);
        changePin = view.findViewById(R.id.change_pin);
        copyIp = view.findViewById(R.id.copy_ip_button);
        updateConnectIdAndPin();
        //WifiManager wm = (WifiManager) getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        refreshHosts();
        //NetWorkUtil.getHostIp());

        setListeners();

        //deviceManger = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        compName = new ComponentName(getActivity(), DeviceAdmin.class);
        //startService(new Intent(MainActivity.this, FloatingView.class));
        return view;
    }

    public JSONArray availableHosts = new JSONArray();
    public void refreshHosts() {
        getActivity().runOnUiThread(() -> {
            TextView ipv4TextView = view.findViewById(R.id.ip_text_view);
            TextView ipv6TextView = view.findViewById(R.id.ip_ipv6_text_view);
            ipv4TextView.setText(NetWorkUtil.getHostIp());


            availableHosts = DiscoverNetIpUtil.getAllPublicInet6HostsAndNames(getActivity());
            updatePublicIp(availableHosts);
            if(availableHosts.size()==0) {
                ipv6TextView.setText(getString(R.string.no_available_public_ip));
                return;
            }
            copyIp.setOnClickListener(e -> {
                StringBuilder ip = new StringBuilder();
                for(int i=0;i<availableHosts.size();i++) {
                    ip.append(availableHosts.getJSONObject(i).getString("ip"));
                    if (i!=availableHosts.size()-1) {
                        ip.append("   ");
                    }
                }
                ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).setText(ip.toString());
            });
            StringBuilder string = new StringBuilder();
            for(int i=availableHosts.size()-1;i>=0;i--) {
                string.append(availableHosts.getJSONObject(i).getString("ip"))
                        .append(" ")
                        .append(availableHosts.getJSONObject(i).getString("niName"))
                        .append("\n");
            }
            ipv6TextView.setText(string.toString());
            /*JSONArray hosts = DiscoverNetIpUtil.getAllPublicInet6HostsAndNames(getActivity());
            if(hosts.size()==0) {
                availableHosts.clear();
                updatePublicIp(availableHosts);
                ipv6TextView.setText(getString(R.string.no_available_public_ip));
                return;
            }
            for(int i=availableHosts.size()-1;i>=0;i--) {
                if(!hosts.contains(availableHosts.getJSONObject(i))) {
                    availableHosts.remove(i);
                }
            }
            for(int i=hosts.size()-1;i>=0;i--) {
                if(availableHosts.contains(hosts.getJSONObject(i))) {
                    hosts.remove(i);
                }
            }
            if(hosts.size()==0) {
                updatePublicIp(availableHosts);
                if(availableHosts.size()==0) {
                    getActivity().runOnUiThread(() -> ipv6TextView.setText(getString(R.string.no_available_public_ip)));
                    return;
                }
                StringBuilder string = new StringBuilder();
                for(int i=availableHosts.size()-1;i>=0;i--) {
                    string.append(availableHosts.getJSONObject(i).getString("ip"))
                            .append(" ")
                            .append(availableHosts.getJSONObject(i).getString("niName"))
                            .append("\n");
                }
                getActivity().runOnUiThread(() -> ipv6TextView.setText(string.toString()));
                return;
            }
            ipv6TextView.setText(getString(R.string.remote_detecting));
            new Thread(() -> {
                JSONArray available = HttpService.helpPingIp(hosts);
                if(available==null) {
                    getActivity().runOnUiThread(() -> ipv6TextView.setText(getString(R.string.failed_to_remote_detect)));
                    return;
                }
                for(int i=0;i<available.size();i++) {
                    if(!availableHosts.contains(available.getJSONObject(i))) {
                        availableHosts.add(available.getJSONObject(i));
                    }
                }
                updatePublicIp(availableHosts);
                if(availableHosts.size()==0) {
                    getActivity().runOnUiThread(() -> ipv6TextView.setText(getString(R.string.no_available_public_ip)));
                    return;
                }
                StringBuilder string = new StringBuilder();
                for(int i=availableHosts.size()-1;i>=0;i--) {
                    string.append(availableHosts.getJSONObject(i).getString("ip"))
                            .append(" ")
                            .append(availableHosts.getJSONObject(i).getString("niName"))
                            .append("\n");
                }
                getActivity().runOnUiThread(() -> ipv6TextView.setText(string.toString()));
            }).start();*/
        });
    }

    private void updatePublicIp(JSONArray availableHosts) {
        new Thread(() -> {
            if (HttpService.publicIp(availableHosts)==null) {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void setListeners() {
        changePin.setOnClickListener(e -> {
            JSONObject request = new JSONObject();
            request.put("type", "changePin");
            ClientHelper.sendMessage(getActivity().getApplicationContext(), request.toJSONString());
        });
        connect.setOnClickListener(e -> {
            JSONObject request = new JSONObject();
            request.put("type", "connect");
            request.put("connectId", connectIdText.getText().toString());
            request.put("connectPin", connectPinText.getText().toString());
            Define.temporaryId = request.getString("connectId");
            Define.temporaryPin = request.getString("connectPin");
            ClientHelper.sendMessage(getActivity().getApplicationContext(), request.toJSONString());
            Define.direct = false;
        });
        connectDirect.setOnClickListener(v -> {
            if(NettyClientDirect.INSTANCE !=null) {
                NettyClientDirect.INSTANCE.interrupt();
            }
            String host = hostText.getText().toString();
            //String portString = portText.getText().toString();
            String portString = String.valueOf(Define.defaultPort);
            if(!Utils.checkPort(portString) || Utils.stringIsEmpty(host)) {
                Toast.makeText(getActivity(), R.string.host_or_port_wrong_format, Toast.LENGTH_SHORT).show();
                return;
            }
            new NettyClientDirect(getActivity(), host, Integer.parseInt(portString)
                    , new NettyClientDirect.OnConnectSuccessCallback() {
                @Override
                public void success() {
                    Define.direct = true;
                    startActivity(new Intent(getActivity(), ControlActivity.class));
                }

                @Override
                public void failed(String err) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.failed_to_connect, Toast.LENGTH_SHORT).show());
                }
            }).start();
        });
        view.findViewById(R.id.refresh_ip_button).setOnClickListener(v -> refreshHosts());
    }

    public void updateConnectIdAndPin() {
        ((TextView)view.findViewById(R.id.connect_id_text_view)).setText(String.valueOf(Define.connectId));
        ((TextView)view.findViewById(R.id.connect_pin_text_view)).setText(String.valueOf(Define.connectPin));
    }



    private void initData() {
        MediaProjectionHelper.getInstance().setNotificationEngine(() -> {
            String title = getString(R.string.service_start);
            return NotificationHelper.getInstance().createSystem()
                    .setOngoing(true)// 常驻通知栏
                    .setTicker(title)
                    .setContentText(title)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LogUtil.i("Environment.isExternalStorageLegacy: " + Environment.isExternalStorageLegacy());
        }
    }
}
