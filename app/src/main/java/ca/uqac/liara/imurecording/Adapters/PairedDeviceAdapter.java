package ca.uqac.liara.imurecording.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleDevice;

import java.util.List;

import ca.uqac.liara.imurecording.R;
import ca.uqac.liara.imurecording.Utils.StringUtils;

/**
 * Created by FlorentinTh on 10/18/2016.
 */

public class PairedDeviceAdapter extends BaseAdapter {

    private Context context;
    private List<RxBleDevice> data;
    private OnUnpairButtonClickListener listener;

    public void setListener(OnUnpairButtonClickListener listener) {
        this.listener = listener;
    }

    public PairedDeviceAdapter(Context context, List<RxBleDevice> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public RxBleDevice getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return data.indexOf(data.get(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.paired_device, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.unpairButton = (Button) convertView.findViewById(R.id.btn_unpair);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RxBleDevice device = getItem(position);

        String deviceName = device.getName();

        if (deviceName == null) {
            deviceName = context.getString(R.string.device_name_unknown);
        }

        holder.name.setText(deviceName);
        holder.unpairButton.setOnClickListener(
                v -> {
                    if (listener != null) {
                        listener.onUnpairButtonClick(position);
                    }
                }
        );

        return convertView;
    }

    public class ViewHolder {
        public TextView name;
        public Button unpairButton;
    }

    public interface OnUnpairButtonClickListener {
        void onUnpairButtonClick(int position);
    }
}
