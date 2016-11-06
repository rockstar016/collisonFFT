package com.example.rock.fft;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.Spinner;
import android.widget.Toast;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static android.R.attr.format;

public class MainActivity extends AppCompatActivity {
    Spinner spinner;
    GraphDrawView imageView;
    Button button;
    Uri selectedFile;
    File mFile;
    private int mSampleRate, mChannels, bytePerSample;
    ///
    private double INTERVAL_TIME_VALUE = 0.2;//// it means 200ms
    int BYTEAMOUNT_INTERVAL;
    double toTransform[];
    /*****
     *  FFT processor
     * ****/
    DoubleFFT_1D transformer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RequirePermission();
    }
    private void RequirePermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},120);
        }
        else {
            initControlls();
        }
    }
    private void initControlls(){
        spinner = (Spinner)findViewById(R.id.spinner);
        imageView = (GraphDrawView) findViewById(R.id.imageView);
        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAudioList();
            }
        });
    }
    private void openAudioList() {
        Intent intent_open = new Intent();
        intent_open.setType("audio/*");
        intent_open.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_open, 300);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 300) {
            if (resultCode == RESULT_OK) {
                selectedFile = data.getData();
                try {
                    loadFromFile();
                }catch(IOException ie){
                    Toast.makeText(this, "Error on loading sound file", Toast.LENGTH_SHORT).show();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private String getRealPathFromURI(Uri contentURI) {
        String result;

        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    public String getFilePath(){
        String filePath;
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
            filePath = getPath(this, selectedFile);
        else
            filePath = getRealPathFromURI(selectedFile);
        mFile = new File(filePath);
        if(!mFile.exists()) {
            Toast.makeText(this, "There's no file.", Toast.LENGTH_SHORT).show();
            return "";
        }
        if (filePath==null)
            return "";
        return filePath;
    }
    private void loadFromFile() throws IOException {
        String filePath = getFilePath();
        getNecessaryDataFromWavFile();
        BYTEAMOUNT_INTERVAL = (int)(INTERVAL_TIME_VALUE * getBytePerSecond());
        makeSpinnerData();

    }
    public int getBytePerSecond(){
        return mSampleRate * bytePerSample * mChannels;
    }
    public void getNecessaryDataFromWavFile() throws IOException{

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(mFile.getPath());
        int numTracks = extractor.getTrackCount();
        MediaFormat format = new MediaFormat();
        int i;
        for (i=0; i<numTracks; i++) {
            format = extractor.getTrackFormat(i);
            if (format.getString(MediaFormat.KEY_MIME).startsWith("audio/")) {
                extractor.selectTrack(i);
                break;
            }
        }
        if (i == numTracks) {
            try {
                throw new Exception("No audio track found in " + mFile);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "File error.", Toast.LENGTH_SHORT).show();
            }
        }
        mChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        bytePerSample = mChannels;
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 120) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
            return;
        }
    }

    public void makeSpinnerData(){
        long sizeOfFile = mFile.length();
        int count_spinner_item =(int) Math.round(sizeOfFile / BYTEAMOUNT_INTERVAL);
        if(sizeOfFile % BYTEAMOUNT_INTERVAL != 0){
            count_spinner_item += 1;
        }
        String[] arrayForSpinner = new String[count_spinner_item+1];
        arrayForSpinner[0] = "";
        for(int i = 1 ; i < count_spinner_item + 1; i++){
            arrayForSpinner[i] = (i + "");
        }
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayForSpinner); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i != 0){
                    try {
                        LoadDataFromSound(i);
                    }catch (Exception e){
                        Log.d("LoadDataFromSound:","Error");
                    }
                }
                else{
                    initDraw();//when selecting 0, it means clearing the drawing area.
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    public void initDraw(){
        imageView.drawClear();
    }


    public void LoadDataFromSound(int index) throws  Exception{
        byte[] byteData = null;
        int amount_read_byte_once = 64;
        byteData = new byte[amount_read_byte_once];

        toTransform = new double[amount_read_byte_once*2];
        transformer = new DoubleFFT_1D(amount_read_byte_once);

        int startoffset = BYTEAMOUNT_INTERVAL * (index - 1);
        FileInputStream in = null;
        in = new FileInputStream( mFile );
        int bytesread = 0, ret = 0;
        int count_numbers = 0;
        in.skip(startoffset);
        double[] peak_frequencies, peak_magnitude;
        int size_of_frequencies = Math.round(BYTEAMOUNT_INTERVAL / amount_read_byte_once);
        peak_frequencies = new double[size_of_frequencies];
        peak_magnitude = new double[size_of_frequencies];
        while (bytesread < BYTEAMOUNT_INTERVAL) {
            //it will be chanaged when you use mic, too.
            //long len = mFile.length();
            try {
                //replace here with your input stream from mic
                ret = in.read(byteData, 0, amount_read_byte_once);
            }catch (Exception e){
                Log.d("test","test");
            }
            for (int i = 0; i < amount_read_byte_once && i < ret; i++) {
                toTransform[i*2] = (short) byteData[i] ;
                toTransform[i*2 + 1] = 0;
            }
            try {
                transformer.complexForward(toTransform);
            }catch (Exception e){
                Log.d("error","FFT error");
            }
            int maxInd = -1;
            double maxMag = Double.NEGATIVE_INFINITY;
            for(int i = 0; i < toTransform.length / 2; ++i) {
                double re  = toTransform[2*i];
                double im  = toTransform[2*i+1];
                double mag = Math.sqrt(re * re + im * im);
                if(mag > maxMag) {
                    maxMag = mag;
                    maxInd = i;
                }
            }
            try {
                peak_magnitude[count_numbers] = maxMag;
                peak_frequencies[count_numbers] = (double)mSampleRate * maxInd / (toTransform.length / 2);
            }catch (Exception e){
                Log.d("exception","exception");
            }
            count_numbers ++;
            if (ret != -1) {
                bytesread += ret;
            } else break;
        }
        DrawGraphBasedFrequencyMagnitude(peak_frequencies, peak_magnitude);
        in.close();
    }

    public void DrawGraphBasedFrequencyMagnitude(double[] peak_frequencies, double[] peak_magnitude){
            //now I have some sample datas with magnitude, frequency, I will draw graph with this datas
            double[] normal_freq_array = new double[peak_frequencies.length];
            double[]  normal_mag_array = new double[peak_magnitude.length];
            imageView.setX(peak_frequencies);
            imageView.setY(peak_magnitude);
            imageView.drawValues();
    }
    public short[] convertByteToShortArray(byte[] byteArray){
        int size = byteArray.length;
        short[] shortArray = new short[size];

        for (int index = 0; index < size; index++)
            shortArray[index] = (short) byteArray[index];
        return shortArray;
    }
}
