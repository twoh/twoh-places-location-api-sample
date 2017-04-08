package id.web.twoh.placesapitutorial;

import android.app.ProgressDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;

import id.web.twoh.placesapitutorial.util.Constants;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private Location mLastLocation;
    private Button btPlacesAPI;
    private Button btLocation;
    private Button btGeocoding;
    private EditText etAlamat;
    private TextView tvPlaceAPI;
    private GoogleApiClient mGoogleApiClient;
    // konstanta untuk mendeteksi hasil balikan dari place picker
    private int PLACE_PICKER_REQUEST = 1;
    private ProgressDialog dialog;
    private GeocoderResultReceiver geocoderReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupGoogleAPI();
        setupDialog();

        geocoderReceiver = new GeocoderResultReceiver(new Handler());

        tvPlaceAPI = (TextView) findViewById(R.id.tv_place_id);

        btPlacesAPI = (Button)findViewById(R.id.bt_ppicker);
        btPlacesAPI.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // membuat Intent untuk Place Picker
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    //menjalankan place picker
                    startActivityForResult(builder.build(MainActivity.this), PLACE_PICKER_REQUEST);

                // check apabila Google Play Services tidak terinstall di HP
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        btLocation = (Button) findViewById(R.id.bt_getLocation);
        btLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null) {
                    Toast.makeText(MainActivity.this," Get Location \n " +
                            "Latitude : "+ mLastLocation.getLatitude()+
                            "\nLongitude : "+mLastLocation.getLongitude(), Toast.LENGTH_LONG).show();
                }
            }
        });

        etAlamat = (EditText) findViewById(R.id.et_alamat);

        btGeocoding = (Button) findViewById(R.id.bt_geoCoding);
        btGeocoding.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
                if(mGoogleApiClient.isConnected()){
                    startIntentService(etAlamat.getText().toString());
                }
            }
        });
    }

    private void setupDialog(){
        dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Loading");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // menangkap hasil balikan dari Place Picker, dan menampilkannya pada TextView
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                String toastMsg = String.format(
                        "Place: %s \n" +
                        "Alamat: %s \n" +
                        "Latlng %s \n", place.getName(), place.getAddress(), place.getLatLng().latitude+" "+place.getLatLng().longitude);
                tvPlaceAPI.setText(toastMsg);
            }
        }
    }

    private void setupGoogleAPI(){
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Toast.makeText(this," Connected to Google Location API", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    protected String address;

    class GeocoderResultReceiver extends ResultReceiver {
        public GeocoderResultReceiver(Handler handler) {
            super(handler);
        }

        /**
         *  Menerima balikan data dari GeocoderIntentService and menampilkan Toast di MainActivity.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Menampilkan alamat, atau error yang didapat dari proses reverse geocoding
            address = resultData.getString(Constants.RESULT_DATA_KEY);

            // Memunculkan toast message jika ada alamat ditemukan
            if (resultCode == Constants.SUCCESS_RESULT) {
                Toast.makeText(MainActivity.this, "Alamat ditemukan \n" +
                        address, Toast.LENGTH_LONG).show();
            }

            dialog.dismiss();
        }
    }

    protected void startIntentService(String alamat) {
        // Membuat intent yang mengarah ke IntentService untuk proses reverse geocoding
        Intent intent = new Intent(this, GeocoderIntentService.class);

        // Mengirim ResultReceiver sebagai extra ke intent service.
        intent.putExtra(Constants.RECEIVER, geocoderReceiver);

        // Mengirim location data sebagai extra juga ke intent service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

        // Cek apakah ada alamat yang diinputkan, jika ada kirim ke intent service
        if(!TextUtils.isEmpty(alamat)){
            intent.putExtra(Constants.ADDRESS_DATA_EXTRA, alamat);
        }

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        // tl;dr = menyalakan intent service :)
        startService(intent);
    }
}
