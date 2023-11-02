package com.example.giaodienchinh_doan.AnotherNav;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.giaodienchinh_doan.Listener.ImageUploadCallback;
import com.example.giaodienchinh_doan.Login;
import com.example.giaodienchinh_doan.R;
import com.example.giaodienchinh_doan.Model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProfileActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "Update email";
    FirebaseAuth auth;
    TextView AddImage;
    Button button;
    TextView textView;
    Button Save;
    FirebaseUser user;
    ImageView ImgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        AddImage=findViewById(R.id.AddImage);

        ImgView=findViewById(R.id.profileImage);


        EditText name = findViewById(R.id.username);
        EditText email = findViewById(R.id.email);
        EditText phone = findViewById(R.id.phone);
        email.setEnabled(false);
        email.setFocusable(false);
        email.setClickable(false);
        AddImage.setOnClickListener(v -> openFileChooser());

        Button buttonLogout = findViewById(R.id.LogOut);
        Save = findViewById(R.id.EditProfile);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        email.setText(user.getEmail());

        if (user != null) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            usersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User userProfile = snapshot.getValue(User.class);
                        if (userProfile != null) {
                            name.setText(userProfile.displayName);
                            phone.setText(userProfile.phoneNumber);
                            email.setText(user.getEmail());
                            Glide.with(EditProfileActivity.this)
                                    .load(userProfile.getI())
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .into(ImgView);
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("TAG", "Error retrieving user information: " + error.getMessage());
                }
            });
        }

        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage(new ImageUploadCallback() {
                    @Override
                    public void onImageUploaded(String imageUrl) {
                        // Sử dụng imageUrl ở đây
                        Log.d(TAG, "Received URL: " + imageUrl);

                        // Cập nhật UrlImg với giá trị mới
                        String UrlImg = imageUrl;
                        Log.d(TAG, "UrlImg after upload: " + UrlImg);

                        // Tạo một đối tượng User mới
                        String mail = email.getText().toString();
                        String displayName = name.getText().toString();
                        String phoneNumber = phone.getText().toString();
                        User newUser = new User(mail, displayName, phoneNumber, user.getUid(), "FCM", UrlImg);

                        // Lưu thông tin người dùng vào cơ sở dữ liệu Firebase
                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                        usersRef.child(user.getUid()).setValue(newUser)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("TAG", "User information saved successfully");
                                    new AlertDialog.Builder(EditProfileActivity.this)
                                            .setTitle("Success")
                                            .setMessage("Data has been saved successfully.")
                                            .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                                                // Thực hiện các hành động tiếp theo tại đây (nếu có)
                                            })
                                            .show();
                                })
                                .addOnFailureListener(e -> Log.e("TAG", "Failed to save user information: " + e.getMessage()));
                    }
                });
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(EditProfileActivity.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
    }
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            // Hiển thị ảnh được chọn lên ImageView
            Glide.with(this).load(imageUri).into(ImgView);
            ImgView.setTag(imageUri);
        }
    }


    private void uploadImage(ImageUploadCallback callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        Uri imageUri = (Uri) ImgView.getTag(); // Lấy Uri từ thẻ tag của ImageView

        if (imageUri != null) {
            StorageReference imagesRef = storageRef.child("images/" + imageUri.getLastPathSegment());
            imagesRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Handle success
                        imagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Lấy URL từ ảnh tải lên
                            String imageUrl = uri.toString();
                            Log.d("Link has been uploaded : ", imageUrl);

                            Log.d(TAG, "Upload successful");
                            callback.onImageUploaded(imageUrl); // Truyền URL về callback
                        }).addOnFailureListener(e -> Log.d(TAG, "Failed to get download URL"));
                    })
                    .addOnFailureListener(exception -> {
                        // Handle any errors
                        Log.d(TAG, "Upload failed");
                    });
        }
    }



}
