package org.mj.leapremote.ui.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.alibaba.fastjson.JSONObject;
import com.google.android.material.tabs.TabLayout;
import com.lxj.xpopup.XPopup;

import org.jetbrains.annotations.NotNull;
import org.mj.leapremote.Define;
import org.mj.leapremote.R;
import org.mj.leapremote.cs.direct.NettyClientDirect;
import org.mj.leapremote.model.Device;
import org.mj.leapremote.service.HttpService;
import org.mj.leapremote.ui.activities.ControlActivity;
import org.mj.leapremote.util.ClientHelper;
import org.mj.leapremote.util.DataUtil;
import org.mj.leapremote.util.DevicesUtil;
import org.mj.leapremote.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainFragment extends Fragment {
    public View view;
    public ListView manualDeviceListView;
    public ListView directDeviceListView;
    public BaseAdapter manualDeviceAdapter;
    public BaseAdapter directDeviceAdapter;
    public Button addDeviceButton;
    private TabLayout tabLayout;
    public ViewPager viewPager;
    public TextView noDeviceText;
    public TextView noDeviceText2;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_main, container, false);
        addDeviceButton = view.findViewById(R.id.add_device_button);
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tabs);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Nullable
            @org.jetbrains.annotations.Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return position==0?getString(R.string.plain_mode):getString(R.string.direct_mode);
            }

            @NotNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                View view = getActivity().getLayoutInflater().inflate(position==0?R.layout.layout_plain_mode:R.layout.layout_direct_mode, null);
                if(position==0) {
                    manualDeviceListView = view.findViewById(R.id.device_list_view);
                    noDeviceText = view.findViewById(R.id.text_no_device);
                    initDataPlain();
                } else {
                    directDeviceListView = view.findViewById(R.id.direct_device_list_view);
                    noDeviceText2 = view.findViewById(R.id.text_no_device_2);
                    initDataDirect();
                }
                container.addView(view ,0);
                return view;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NotNull Object object) {
                return view==object;
            }
        });
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.normal);
        tabLayout.getTabAt(1).setIcon(R.drawable.auto_connect);
        initListeners();
        return view;
    }

    public List<Device> data1;
    public List<Device> data2;
    private void initDataPlain() {
        data1 = new ArrayList<>(Arrays.asList(Define.plainDevices.toArray(new Device[]{})));

        manualDeviceAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data1.size();
            }

            @Override
            public Object getItem(int position) {
                return data1.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_device, parent, false);
                Button actionsButton = view.findViewById(R.id.actions_button);
                ((ImageView)view.findViewById(R.id.device_image_view)).setImageResource(R.mipmap.ic_launcher);
                ((TextView)view.findViewById(R.id.device_name_text_view)).setText(data1.get(position).getName());
                ((TextView)view.findViewById(R.id.device_status_text_view)).setText(data1.get(position).getStatusString(getResources()));
                ((TextView)view.findViewById(R.id.device_status_text_view)).setTextColor(data1.get(position).getStatusColor());
                actionsButton.setOnClickListener(v -> {
                    if(actionsButton.isEnabled()) {
                        Device device = data1.get(position);
                        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            switch (item.getItemId()){
                                case R.id.item_control:
                                    switch (device.getStatus()) {
                                        case Device.STATUS_ONLINE:
                                        case Device.STATUS_DIRECT:
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
                });
                return view;
            }
        };
        manualDeviceListView.setAdapter(manualDeviceAdapter);
        refreshViewPlain();
    }

    public void initDataDirect() {
        data2 = new ArrayList<>(Arrays.asList(Define.directDevices.toArray(new Device[]{})));
        directDeviceAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data2.size();
            }

            @Override
            public Object getItem(int position) {
                return data2.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_device, parent, false);
                Button actionsButton = view.findViewById(R.id.actions_button);
                ((ImageView)view.findViewById(R.id.device_image_view)).setImageResource(R.mipmap.ic_launcher);
                ((TextView)view.findViewById(R.id.device_name_text_view)).setText(data2.get(position).getName());
                ((TextView)view.findViewById(R.id.device_status_text_view)).setText(data2.get(position).getStatusString(getResources()));
                ((TextView)view.findViewById(R.id.device_status_text_view)).setTextColor(data2.get(position).getStatusColor());
                actionsButton.setOnClickListener(v -> {
                    if(actionsButton.isEnabled()) {
                        Device device = data2.get(position);
                        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                        popupMenu.setOnMenuItemClickListener(item -> {
                            switch (item.getItemId()){
                                case R.id.item_control:
                                    if(NettyClientDirect.INSTANCE !=null) {
                                        NettyClientDirect.INSTANCE.interrupt();
                                    }
                                    new NettyClientDirect(getActivity(), device.getIp(), device.getPort()
                                            , new NettyClientDirect.OnConnectSuccessCallback() {
                                        @Override
                                        public void success() {
                                            Define.direct = true;
                                            startActivity(new Intent(getActivity(), ControlActivity.class));
                                        }
                                        @Override
                                        public void failed(String err) {
                                            System.err.println(err);
                                            getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.failed_to_connect, Toast.LENGTH_SHORT).show());
                                        }
                                    }).start();
                                    return true;
                                case R.id.item_edit:
                                    new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.edit),
                                            "请输入主机",
                                            device.getIp(),
                                            "请输入主机",
                                            host -> {
                                                if(Utils.stringIsEmpty(host)) {
                                                    new XPopup.Builder(getActivity()).asConfirm("失败", "主机不能为空", null).show();
                                                    return;
                                                }
                                    /*new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.direct_mode), "请输入端口",
                                            port -> {*/
                                                String port = String.valueOf(Define.defaultPort);
                                                if(Utils.stringIsEmpty(port)) {
                                                    new XPopup.Builder(getActivity()).asConfirm("失败", "端口不能为空", null).show();
                                                    return;
                                                }
                                                if(!Utils.checkPort(port)) {
                                                    new XPopup.Builder(getActivity()).asConfirm("失败", "端口只能为1~65535", null).show();
                                                    return;
                                                }
                                                Device d = new Device();
                                                d.setName(host);
                                                d.setStatus(Device.STATUS_NOT_SUPPORTED);
                                                d.setMode(Device.MODE_DIRECT);
                                                d.setIp(host);
                                                d.setPort(Integer.parseInt(port));
                                                Define.directDevices.set(position, d);
                                                DataUtil.save();
                                                refreshDevices();
                                                //}).show();
                                            }).show();
                                    return true;
                                case R.id.item_remove:
                                    Define.directDevices.remove(position);
                                    DataUtil.save();
                                    refreshDevices();
                                    return true;
                                default:
                                    return false;
                            }
                        });
                        popupMenu.getMenuInflater().inflate(R.menu.menu_device_actions, popupMenu.getMenu());
                        popupMenu.show();
                    }
                });
                return view;
            }
        };
        directDeviceListView.setAdapter(directDeviceAdapter);
        refreshViewDirect();
    }

    public void refreshDevices() {
        getActivity().runOnUiThread(() -> {
            data1 = new ArrayList<>(Arrays.asList(Define.plainDevices.toArray(new Device[]{})));
            data2 = new ArrayList<>(Arrays.asList(Define.directDevices.toArray(new Device[]{})));
            manualDeviceAdapter.notifyDataSetChanged();
            directDeviceAdapter.notifyDataSetChanged();
            refreshView();
        });
    }

    private void refreshView() {
        refreshViewPlain();
        refreshViewDirect();
    }

    private void refreshViewPlain() {
        noDeviceText.setVisibility(data1.size()==0?View.VISIBLE:View.GONE);
    }

    private void refreshViewDirect() {
        noDeviceText2.setVisibility(data2.size()==0?View.VISIBLE:View.GONE);
    }

    private void initListeners() {
        addDeviceButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getActivity(), v);
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()){
                    case R.id.manual_item:
                        new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.plain_mode), getString(R.string.input_connect_id),
                                id -> {
                                    if(Utils.stringIsEmpty(id)) {
                                        new XPopup.Builder(getActivity()).asConfirm(getString(R.string.failed), getString(R.string.id_cannot_be_empty), null).show();
                                        return;
                                    }
                                    if(DevicesUtil.getDeviceFromDevices(id)!=null) {
                                        new XPopup.Builder(getActivity()).asConfirm(getString(R.string.failed), getString(R.string.device_existed), null).show();
                                        return;
                                    }
                                    new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.plain_mode), getString(R.string.input_connect_pin),
                                            pin -> new Thread(() -> {
                                                Device device = HttpService.getDevice(id, pin);
                                                if(device==null) {
                                                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.cannot_connect_to_server, Toast.LENGTH_SHORT).show());
                                                    return;
                                                }
                                                if(device.getDeviceId()==null) {
                                                    getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), R.string.no_such_device, Toast.LENGTH_SHORT).show());
                                                    return;
                                                }
                                                Define.plainDevices.add(device);
                                                getActivity().runOnUiThread(() -> tabLayout.selectTab(tabLayout.getTabAt(0)));
                                                DevicesUtil.updateDevicesWithLocal(getActivity());
                                            }).start())
                                            .show();
                                }).show();
                        return true;
                    case R.id.direct_item:
                        new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.direct_mode), getString(R.string.input_host),
                                host -> {
                                    if(Utils.stringIsEmpty(host)) {
                                        new XPopup.Builder(getActivity()).asConfirm(getString(R.string.failed), getString(R.string.host_cannot_be_empty), null).show();
                                        return;
                                    }
                                    /*new XPopup.Builder(getActivity()).asInputConfirm(getString(R.string.direct_mode), "请输入端口",
                                            port -> {*/
                                    String port = String.valueOf(Define.defaultPort);
                                    if(Utils.stringIsEmpty(port)) {
                                        new XPopup.Builder(getActivity()).asConfirm(getString(R.string.failed), getString(R.string.port_cannot_be_empty), null).show();
                                        return;
                                    }
                                    if(!Utils.checkPort(port)) {
                                        new XPopup.Builder(getActivity()).asConfirm(getString(R.string.failed), getString(R.string.port_range_limit), null).show();
                                        return;
                                    }
                                    Device device = new Device();
                                    device.setName(host);
                                    device.setStatus(Device.STATUS_NOT_SUPPORTED);
                                    device.setMode(Device.MODE_DIRECT);
                                    device.setIp(host);
                                    device.setPort(Integer.parseInt(port));
                                    Define.directDevices.add(device);
                                    DataUtil.save();
                                    getActivity().runOnUiThread(() -> tabLayout.selectTab(tabLayout.getTabAt(1)));
                                    refreshDevices();
                                    //}).show();
                                }).show();
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.getMenuInflater().inflate(R.menu.menu_add_device, popupMenu.getMenu());
            popupMenu.show();
        });
    }
}
