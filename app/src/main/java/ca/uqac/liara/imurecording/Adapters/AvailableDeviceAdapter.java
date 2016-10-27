package ca.uqac.liara.imurecording.Adapters;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import ca.uqac.liara.imurecording.R;

/**
 * Created by FlorentinTh on 10/14/2016.
 */

public class AvailableDeviceAdapter extends BaseAdapter {

    private Context context;
    private List<BluetoothDevice> data;
    private BluetoothDevice pairedDevice;

    public AvailableDeviceAdapter(Context context, List<BluetoothDevice> data) {
        this.context = context;
        this.data = data;
    }

    public void addDevice(BluetoothDevice device) {
        if (!(data.contains(device))) {
            if (pairedDevice != null) {
                if (!device.equals(pairedDevice)) {
                    data.add(device);
                }
            } else {
                data.add(device);
            }
        }
    }

    public void removeDevice(BluetoothDevice device) {
        if (data.contains(device)) {
            data.remove(device);
        }
    }

    public void setPairedDevice(BluetoothDevice device) {
        pairedDevice = device;
    }

    public BluetoothDevice getDevice(int position) {
        return data.get(position);
    }

    public void clear() {
        data.clear();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.indexOf(getItem(position));
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.available_device, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.address = (TextView) convertView.findViewById(R.id.address);
            holder.connectionState = (TextView) convertView.findViewById(R.id.connection_state);
            holder.progress = (ProgressBar) convertView.findViewById(R.id.progress);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        BluetoothDevice device = getDevice(position);

        String deviceName = device.getName();

        if (device.getName() == null) {
            deviceName = context.getString(R.string.device_name_unknown);
        }

        holder.name.setText(deviceName);
        holder.address.setText("[" + device.getAddress() + "]");
        holder.connectionState.setText(R.string.default_connection_state_value);
        holder.connectionState.setTextColor(context.getResources().getColor(R.color.colorDelete));
        holder.progress.setVisibility(View.INVISIBLE);

        return convertView;
    }

    public class ViewHolder {
        public TextView name;
        public TextView address;
        public TextView connectionState;
        public ProgressBar progress;
    }
}
