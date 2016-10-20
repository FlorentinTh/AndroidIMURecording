package ca.uqac.liara.imurecording.Adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;

import java.util.List;

import ca.uqac.liara.imurecording.R;

/**
 * Created by FlorentinTh on 10/18/2016.
 */

public class PairedDeviceAdapter extends BaseSwipeAdapter {

    private Context context;
    private List<BluetoothDevice> data;
    private OnUseButtonClickListener useListener;
    private OnUnpairButtonClickListener unpairListener;

    public void setOnUseButtonClickListener(@Nullable OnUseButtonClickListener listener) {
        this.useListener = listener;
    }

    public void setOnUnpairButtonClickListener(@Nullable OnUnpairButtonClickListener listener) {
        this.unpairListener = listener;
    }

    public PairedDeviceAdapter(Context context, List<BluetoothDevice> data) {
        this.context = context;
        this.data = data;
    }

    public void addDevice(BluetoothDevice device) {
        data.add(device);
    }

    public BluetoothDevice getDevice(int position) {
        return data.get(position);
    }

    public void clear() {
        data.clear();
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe_layout;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        ViewHolder holder;
        View view = LayoutInflater.from(context).inflate(R.layout.paired_device, null);
        SwipeLayout swipeLayout = (SwipeLayout) view.findViewById(getSwipeLayoutResourceId(position));

        swipeLayout.getDragEdgeMap().clear();
        swipeLayout.addDrag(SwipeLayout.DragEdge.Left, view.findViewById(R.id.bottom_wrapper));

        swipeLayout.addSwipeListener(new SimpleSwipeListener() {


            @Override
            public void onOpen(SwipeLayout layout) {
                YoYo.with(Techniques.Tada).duration(500).delay(100).playOn(layout.findViewById(R.id.trash));
            }
        });

        holder = new ViewHolder();
        holder.name = (TextView) view.findViewById(R.id.name);
        holder.useButton = (Button) view.findViewById(R.id.btn_use);
        holder.unpairButton = (Button) view.findViewById(R.id.btn_unpair);
        view.setTag(holder);

        BluetoothDevice device = getDevice(position);

        String deviceName = device.getName();

        if (deviceName == null) {
            deviceName = context.getString(R.string.device_name_unknown);
        }

        holder.name.setText(deviceName);

        holder.useButton.setOnClickListener(
                v -> {
                    if (useListener != null) {
                        useListener.onUseButtonClick(position);
                    }
                }
        );

        holder.unpairButton.setOnClickListener(
                v -> {
                    if (unpairListener != null) {
                        unpairListener.OnUnpairButtonClick(position);
                    }
                }
        );

        return view;
    }

    @Override
    public void fillValues(int position, View convertView) {}

    @Override
    public int getCount() { return data.size(); }

    @Override
    public Object getItem(int position) { return data.get(position); }
    
    @Override
    public long getItemId(int position) { return data.indexOf(data.get(position)); }

    public class ViewHolder {
        public TextView name;
        public Button useButton;
        public Button unpairButton;
    }

    public interface OnUseButtonClickListener {
        void onUseButtonClick(int position);
    }

    public interface OnUnpairButtonClickListener {
        void OnUnpairButtonClick(int position);
    }
}
