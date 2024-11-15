package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<DeviceData> devices;
    private final String unit;

    public DeviceAdapter(List<DeviceData> devices, String unit) {
        this.devices = devices;
        this.unit = unit;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        DeviceData device = devices.get(position);
        holder.tvDeviceName.setText(device.getName());


        if ("kWt".equals(unit)) {
            holder.tvUsage.setText(String.format("average: %.2f kWt", device.getUsagePercentage()));
        } else if ("percentage".equals(unit)) {
            holder.tvUsage.setText(String.format("%.2f%%", device.getUsagePercentage()));
        }
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvUsage;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.deviceNameTextView);
            tvUsage = itemView.findViewById(R.id.deviceUsageTextView);
        }
    }
}