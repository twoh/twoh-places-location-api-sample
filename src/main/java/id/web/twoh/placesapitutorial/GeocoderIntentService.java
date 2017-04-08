package id.web.twoh.placesapitutorial;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import id.web.twoh.placesapitutorial.util.Constants;

/**
 * Created by Hafizh Herdi on 2/12/2017.
 */

public class GeocoderIntentService extends IntentService {

    private static final String TAG = GeocoderIntentService.class.getSimpleName();

    protected ResultReceiver mReceiver;

    public GeocoderIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        // Cek apakah ada receiver untuk menerima hasil
        if (mReceiver == null) {
            Log.wtf(TAG, "Tidak ada receiver, sehingga hasil tidak bisa dikirim");
            return;
        }

        String alamat = intent.getStringExtra(Constants.ADDRESS_DATA_EXTRA);

        if(!TextUtils.isEmpty(alamat)){
            geoCoding(alamat);
        }else{
            reverseGeocoding(intent);
        }
    }

    private void geoCoding(String alamat){
        String errorMessage = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName(alamat, 1);
        } catch (IOException ioException) {
            // Menangkap apabila ada I/O atau jaringan error
            errorMessage = "Location Service is not available";
            ioException.printStackTrace();
            Log.e(TAG, errorMessage, ioException);
        }

        // Apabila tidak ada alamat yang bisa ditemukan
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "Koordinat tidak ditemukan";
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            // Mendapatkan hasil dari geocoding alamat, dan ambil lat long nya
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();
            addressFragments.add(address.getFeatureName());
            addressFragments.add(String.valueOf(address.getLatitude()));
            addressFragments.add(String.valueOf(address.getLongitude()));

            Log.i(TAG, "alamat ditemukan");
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }
    }

    private void reverseGeocoding(Intent intent){
        String errorMessage = "";
        // Mendapatkan location yang dikirim ke intent service lewat extra
        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        // Make sure jika location yang dikirim tidak null
        if (location == null) {
            errorMessage = "Location tidak ditemukan";
            Log.wtf(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {

            /**
             * Proses reverse geocoding
             */
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException ioException) {
            // Menangkap apabila ada I/O atau jaringan error
            errorMessage = "Location Service is not available";
            ioException.printStackTrace();
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Apabila invalid latitude longitude
            errorMessage = "Invalid latitude longitude";
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        // Apabila tidak ada alamat yang bisa ditemukan
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = "Alamat tidak ditemukan";
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            /**
             * Kalian juga bisa mendapatkan detail alamat lainnya
             * seperti kelurahan, kecamatan, kodepos dsb dalam istilah Inggrisnya
             * Contoh :
             * getLocality() ("Mountain View", for example)
             * getAdminArea() ("CA", for example)
             * getPostalCode() ("94043", for example)
             * getCountryCode() ("US", for example)
             * getCountryName() ("United States", for example)
             */

            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, "alamat ditemukan");
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }
    }

    /**
     * Sends a resultCode and message to the receiver.
     */
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

}
