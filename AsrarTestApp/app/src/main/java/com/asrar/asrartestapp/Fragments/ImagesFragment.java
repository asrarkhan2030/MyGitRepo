package com.asrar.asrartestapp.Fragments;


import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.asrar.asrartestapp.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 */
public class ImagesFragment extends Fragment
{

    View rootView;
    @BindView(R.id.imgCard)
    ImageView imgCard;
    @BindView(R.id.imgCard1)
    ImageView imgCard1;
    @BindView(R.id.imgCard2)
    ImageView imgCard2;
    @BindView(R.id.imgCard3)
    ImageView imgCard3;

    @BindView(R.id.txtSize)
    TextView txtSize;
    @BindView(R.id.txtSize1)
    TextView txtSize1;
    @BindView(R.id.txtSize2)
    TextView txtSize2;
    @BindView(R.id.txtSize3)
    TextView txtSize3;

    @BindView(R.id.btnSubmit)
    Button btnSubmit;

    File[] files = new File[4];

    private AlertDialog mAlertDialog;
    private static final int PICK_FROM_CAMERA = 101;
    private File imageFile;
    String picturePath;
    public static final String DIRECTORY_NAME = "AsrarTestApp";

    String fileName ;

    static int i;

    private static final String BUCKET_NAME = "araay";
    private static final int REQUEST_SELECT_PICTURE = 0x01;
    protected static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    protected static final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;
    CognitoCachingCredentialsProvider credentialsProvider;
    TransferUtility transferUtility;


    public ImagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_images, container, false);
        ButterKnife.bind(this , rootView);
        for(int j=0 ; j<4 ;j++) {
            files[j]=null;
        }

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity(), "ap-northeast-1:9ad254c6-85fd-4e97-891f-0928b9b0d657", Regions.AP_NORTHEAST_1 );

        AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3, getActivity());

        imgCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                i=0;
                selectImages();
            }
        });

        imgCard1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                i=1;
                selectImages();
            }
        });

        imgCard2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                i=2;
                selectImages();
            }
        });

        imgCard3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                i=3;
                selectImages();
            }
        });

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final TransferObserver[] observer = new TransferObserver[1];
                Handler handler1 = new Handler();
                boolean isEmpty = true;
                for(int j = 0 ; j<files.length; j++)
                {
                    if(!(files[j]== null)) {
                        observer[0] = transferUtility.upload(
                                            BUCKET_NAME,
                                            files[j].getName(),
                                            files[j]
                                    );
                        isEmpty = false;
                    }
                    if(!isEmpty && j == files.length-1)
                    {
                        handler1.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                String SDCard = Environment.getExternalStorageDirectory().toString();
                                File appDir = new File(SDCard, DIRECTORY_NAME);
                                deleteNon_EmptyDir(appDir);
                                imgCard.setImageResource(R.mipmap.bg);
                                imgCard1.setImageResource(R.mipmap.bg);
                                imgCard2.setImageResource(R.mipmap.bg);
                                imgCard3.setImageResource(R.mipmap.bg);
                                txtSize.setText("");
                                txtSize1.setText("");
                                txtSize2.setText("");
                                txtSize3.setText("");
                                Toast.makeText(getActivity() , "Images Uploaded Successfully" , Toast.LENGTH_LONG).show();
                            }
                        }, 3000);
                    }
                }
            }
        });

        return rootView;
    }

    private void selectImages() {
        try {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View promptView = layoutInflater.inflate(R.layout.select_image, null);

            final AlertDialog alertD = new AlertDialog.Builder(getActivity()).create();

            final ImageView imgCamera = (ImageView) promptView.findViewById(R.id.camera);
            ImageView imgGallery = (ImageView) promptView.findViewById(R.id.gallery);
            checkForPermission();

            imgCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri tempUri = null;
                    //Log.e(TAG, "camera");
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String SDCard = Environment.getExternalStorageDirectory().toString();
                    File appDir = new File(SDCard, DIRECTORY_NAME);

                    if(!appDir.exists())
                        appDir.mkdir();

                    imageFile = new File(SDCard, DIRECTORY_NAME + "/ali_"
                            + timeStamp + ".jpg");

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        tempUri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", imageFile);
                    }else{
                        tempUri = Uri.fromFile(imageFile);
                    }
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, tempUri);
                    intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                    startActivityForResult(intent, PICK_FROM_CAMERA);
                    alertD.cancel();
                }
            });

            imgGallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    pickFromGallery();
                    alertD.cancel();
                }
            });

            alertD.setView(promptView);

            alertD.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkForPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (getActivity().checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    //Log.e("testing", "Permission is granted");
                } else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]
                            {android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                    android.Manifest.permission.CAMERA}, 1);
                    //Log.e("testing", "Permission is revoked");
                }
            } else { //permission is automatically granted on sdk<23 upon installation
                //Log.e("testing", "Permission is already granted");

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.permission_read_storage_rationale),
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_SELECT_PICTURE);
        }
    }

    protected void requestPermission(final String permission, String rationale, final int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {
            showAlertDialog(getString(R.string.permission_title_rationale), rationale,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(getActivity(),
                                    new String[]{permission}, requestCode);
                        }
                    }, getString(R.string.label_ok), null, getString(R.string.label_cancel));
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
        }
    }

    protected void showAlertDialog(@Nullable String title, @Nullable String message,
                                   @Nullable DialogInterface.OnClickListener onPositiveButtonClickListener,
                                   @NonNull String positiveText,
                                   @Nullable DialogInterface.OnClickListener onNegativeButtonClickListener,
                                   @NonNull String negativeText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, onPositiveButtonClickListener);
        builder.setNegativeButton(negativeText, onNegativeButtonClickListener);
        mAlertDialog = builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {


            case REQUEST_SELECT_PICTURE:
                try {

                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                getString(R.string.permission_write_storage_rationale),
                                REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
                    } else {

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Uri selectedImage = data.getData();
                        String[] filePath = {MediaStore.Images.Media.DATA};
                        Cursor c = getActivity().getContentResolver().query(selectedImage, filePath, null, null, null);
                        c.moveToFirst();
                        int columnIndex = c.getColumnIndex(filePath[0]);
                        picturePath = c.getString(columnIndex);
                        imageFile = new File(picturePath);
                        String fileName = imageFile.getName();
                        c.close();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        compressImage( picturePath , fileName );
//                        Toast.makeText(getActivity() , "Image has been successfully added", Toast.LENGTH_LONG).show();
                    }

                } catch (Exception ex) {

                }
                break;

            case PICK_FROM_CAMERA:

                if (resultCode == Activity.RESULT_OK) {
                    if (imageFile.exists()) {
                        try {
                            picturePath = imageFile.getAbsolutePath();
                            String fileName = imageFile.getName();
                            compressImage(picturePath , fileName);
//                            Toast.makeText(getActivity() , "Image has been successfully added", Toast.LENGTH_LONG).show();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else {
                        Toast.makeText(getActivity(), "There was an error saving the file..", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public String compressImage(String filePath , String fileName) {

        Bitmap scaledBitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int picActualHeight = options.outHeight;
        int picActualWidth = options.outWidth;
        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;


        float maxHeight = 816.0f;
        float maxWidth = 1000.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {               imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            }
            else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);
        options.inJustDecodeBounds = false;

        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight,Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(i==0)
        {
            imgCard.setImageBitmap(scaledBitmap);
            txtSize.setText("AS : " + picActualWidth + " x " + picActualHeight + " , RS : " + options.outWidth + " x " + options.outHeight);
        }
        else if(i==1)
        {
            imgCard1.setImageBitmap(scaledBitmap);
            txtSize1.setText("AS : " + picActualWidth + " x " + picActualHeight + " , RS : " + options.outWidth + " x " + options.outHeight);
        }
        else if(i==2)
        {
            imgCard2.setImageBitmap(scaledBitmap);
            txtSize2.setText("AS : " + picActualWidth + " x " + picActualHeight + " , RS : " + options.outWidth + " x " + options.outHeight);
        }
        else if(i==3)
        {
            imgCard3.setImageBitmap(scaledBitmap);
            txtSize3.setText("AS : " + picActualWidth + " x " + picActualHeight + " , RS : " + options.outWidth + " x " + options.outHeight);
//            txtSize3.setText(getString(R.string.format_crop_result_d_d, options.outWidth, options.outHeight));
        }

        FileOutputStream out = null;
        String filename = getFilename();
        try {
            out = new FileOutputStream(filename);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            File myFile = new File(filename);
            files[i] = myFile;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return filename;

    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height/ (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap)
        {
            inSampleSize++;
        }

        return inSampleSize;
    }

    public String getFilename() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String SDCard = Environment.getExternalStorageDirectory().toString();
        File appDir = new File(SDCard, DIRECTORY_NAME);

        if(!appDir.exists())
            appDir.mkdir();

        fileName = "ali_" + timeStamp + ".jpg";

        String uriSting = (appDir.getAbsolutePath() + "/" + fileName);
        return uriSting;

    }

    public static boolean deleteNon_EmptyDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteNon_EmptyDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

}
