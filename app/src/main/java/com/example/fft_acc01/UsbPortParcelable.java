package com.example.fft_acc01;

import android.os.Parcel;
import android.os.Parcelable;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class UsbPortParcelable implements Parcelable {

    public UsbSerialPort usb_port;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.usb_port);

    }

    // Constructor without arguments
    public UsbPortParcelable() {
    }

    // Constructor with argument
    protected UsbPortParcelable(UsbSerialPort port) {
        this.usb_port = port;
    }

    public static final Creator<UsbPortParcelable> CREATOR = new Creator<UsbPortParcelable>() {
        @Override
        public UsbPortParcelable createFromParcel(Parcel source) {
            final UsbPortParcelable loc = new UsbPortParcelable();
            loc.usb_port = (UsbSerialPort)source.readValue(UsbSerialPort.class.getClassLoader());
            return loc;
        }

        @Override
        public UsbPortParcelable[] newArray(int size) {
            return new UsbPortParcelable[size];
        }
    };
}
