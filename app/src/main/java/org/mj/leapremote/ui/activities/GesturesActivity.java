package org.mj.leapremote.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lxj.xpopup.XPopup;

import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.service.AutoService;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.Utils;

public class GesturesActivity extends AppCompatActivity {

    public ListView gestureListView;
    public BaseAdapter gestureAdapter;
    public Button addGestureButton;


    public JSONArray data;

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestures);
        gestureListView = findViewById(R.id.gesture_list_view);
        addGestureButton = findViewById(R.id.add_gesture_button);
        initListeners();
        initData();
    }

    private void initListeners() {
        addGestureButton.setOnClickListener(e -> {
            AutoService.mService.initView(gesture -> {
                new XPopup.Builder(this).asInputConfirm(getString(R.string.add_gesture), getString(R.string.input_name),
                        name -> {
                            if(Utils.stringIsEmpty(name)) {
                                new XPopup.Builder(this).asConfirm(getString(R.string.failed), getString(R.string.name_cannot_be_empty), null).show();
                                return;
                            }
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("name", name);
                            jsonObject.put("gesture", gesture);
                            JSONArray jsonArray = JSONArray.parseArray(Define.savedGestures);
                            jsonArray.add(jsonObject);
                            Define.savedGestures = jsonArray.toString();
                            runOnUiThread(this::initData);
                            new Thread(DataUtil::save).start();
                        }).show();
            });
        });
    }

    private void initData() {
        data = JSONArray.parseArray(Define.savedGestures);
        gestureAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public Object getItem(int position) {
                return data.getJSONObject(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = getLayoutInflater().inflate(R.layout.item_gesture, parent, false);
                Button actionsButton = view.findViewById(R.id.actions_button);
                ((TextView)view.findViewById(R.id.gesture_name_text_view)).setText(data.getJSONObject(position).getString("name"));
                actionsButton.setOnClickListener(v -> {
                    if(actionsButton.isEnabled()) {
                        JSONObject gesture = data.getJSONObject(position);
                        PopupMenu popupMenu = new PopupMenu(GesturesActivity.this, v);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            switch (item.getItemId()){
                                case R.id.item_execute:
                                    AutoService.mService.performMultipleGestures(gesture.getJSONArray("gesture"));
                                    return true;
                                case R.id.item_remove:
                                    data.remove(position);
                                    Define.savedGestures = data.toString();
                                    initData();
                                    new Thread(DataUtil::save).start();
                                    return true;
                                default:
                                    return false;
                            }
                        });
                        popupMenu.getMenuInflater().inflate(R.menu.menu_gesture_actions, popupMenu.getMenu());
                        popupMenu.show();
                    }
                });
                /*View view = getLayoutInflater().inflate(R.layout.item_device, parent, false);
                Button actionsButton = view.findViewById(R.id.actions_button);
                ((ImageView)view.findViewById(R.id.device_image_view)).setImageResource(R.mipmap.ic_launcher);
                ((TextView)view.findViewById(R.id.device_name_text_view)).setText(data.get(position).getName());
                ((TextView)view.findViewById(R.id.device_status_text_view)).setText(data.get(position).getStatusString(getResources()));
                ((TextView)view.findViewById(R.id.device_status_text_view)).setTextColor(data.get(position).getStatusColor());
                actionsButton.setOnClickListener(v -> {
                    if(actionsButton.isEnabled()) {
                        Device device = data.get(position);
                        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            switch (item.getItemId()){
                                case R.id.item_control:
                                    switch (device.getStatus()) {
                                        case Device.STATUS_ONLINE:
                                            JSONObject request = new JSONObject();
                                            request.put("type", "connect");
                                            request.put("connectId", device.getConnectId());
                                            request.put("connectPin", device.getConnectPin());
                                            Define.temporaryId = request.getString("connectId");
                                            Define.temporaryPin = request.getString("connectPin");
                                            ClientHelper.sendMessage(getActivity().getApplicationContext(), request.toJSONString());
                                            Define.direct = false;
                                            break;
                                        case Device.STATUS_OFFLINE:
                                            Toast.makeText(getActivity(), "此设备离线", Toast.LENGTH_SHORT).show();
                                            break;
                                        case Device.STATUS_NOT_ENABLED:
                                            Toast.makeText(getActivity(), "此设备未启用远程控制", Toast.LENGTH_SHORT).show();
                                            break;
                                        default:
                                            Toast.makeText(getActivity(), "此设备状态码错误: "+device.getStatusString(getResources()), Toast.LENGTH_SHORT).show();
                                    }
                                    return true;
                                case R.id.item_edit:
                                    new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.edit),
                                            "当前连接id："+device.getConnectId()+"\n"+"请输入连接密码",
                                            device.getConnectPin(),
                                            "请输入连接密码",
                                            pin -> new Thread(() -> {
                                                Device d = HttpService.getDevice(device.getConnectId(), pin);
                                                if(d==null) {
                                                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show());
                                                    return;
                                                }
                                                if(d.getDeviceId()==null) {
                                                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.no_such_device, Toast.LENGTH_SHORT).show());
                                                    return;
                                                }
                                                Define.plainDevices.add(d);
                                                DevicesUtil.updateDevicesWithLocal(getActivity());
                                            }).start())
                                            .show();
                                    return true;
                                case R.id.item_remove:
                                    JSONObject request = new JSONObject();
                                    request.put("type", "deviceRemove");
                                    request.put("deviceId", device.getDeviceId());
                                    ClientHelper.sendMessage(getActivity().getApplicationContext(), request.toJSONString());
                                    return true;
                                default:
                                    return false;
                            }
                        });
                        popupMenu.getMenuInflater().inflate(R.menu.menu_device_actions, popupMenu.getMenu());
                        popupMenu.show();
                    }
                });*/
                return view;
            }
        };
        gestureListView.setAdapter(gestureAdapter);
        refreshListText();
    }

    private void refreshListText() {
        findViewById(R.id.text_no_gestures).setVisibility(data.size()==0?View.VISIBLE:View.GONE);
    }
}
