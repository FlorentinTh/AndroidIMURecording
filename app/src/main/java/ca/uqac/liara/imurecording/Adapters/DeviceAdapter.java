package ca.uqac.liara.imurecording.Adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import org.w3c.dom.Text;

import java.util.List;

import ca.uqac.liara.imurecording.R;
import ca.uqac.liara.imurecording.Utils.StringUtils;

/**
 * Created by FlorentinTh on 10/14/2016.
 */

public class DeviceAdapter extends BaseAdapter {

    private Context context;
    private List<RxBleDevice> data;

    public DeviceAdapter(Context context, List<RxBleDevice> data) {
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
        return data.indexOf(getItem(position));
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.address = (TextView) convertView.findViewById(R.id.address);
            holder.connectionState = (TextView) convertView.findViewById(R.id.connection_state);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RxBleDevice device = (RxBleDevice) getItem(position);
        holder.name.setText(device.getName());
        holder.address.setText(device.getMacAddress());

        String connectionStateValue = StringUtils.getConnectionStateDescription(device.getConnectionState().toString());
        StringUtils.setConnectionStateValue(holder.connectionState, connectionStateValue);

        return convertView;
    }

    public class ViewHolder {
        public TextView name;
        public TextView address;
        public TextView connectionState;
    }
}
