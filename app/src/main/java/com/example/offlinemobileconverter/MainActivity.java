package com.example.offlinemobileconverter;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private static final int FILE_PICKER_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button convertButton = findViewById(R.id.convertButton);
        statusText = findViewById(R.id.statusText);

        // Uygulama açıldığında izin kontrolü yapalım
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // Butona basınca artık dosya seçici açılacak
        convertButton.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Hata: Önce dosya izni vermelisiniz!");
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
            openFilePicker();
        });
    }

    // 1. Dosya Seçici Ekranını Açan Metod
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*"); // Sadece videoları göster
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Dönüştürülecek Videoyu Seçin"), FILE_PICKER_REQUEST_CODE);
    }

    // 2. Seçilen Dosyayı Yakalayan Metod
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri selectedVideoUri = data.getData();
            String realPath = getPathFromUri(selectedVideoUri);

            if (realPath != null) {
                statusText.setText("Seçildi: " + new File(realPath).getName());
                startConversion(realPath);
            } else {
                statusText.setText("Hata: Dosya yolu alınamadı!");
            }
        }
    }

    // 3. Android URI'sini Gerçek Dosya Yoluna Çeviren Sihirli Metod (Android 9 Uyumlu)
// 3. Kurşun Geçirmez Yol: Dosyayı Güvenli Alana Kopyala
    private String getPathFromUri(Uri uri) {
        String filePath = null;
        try {
            // Güvenli alanımızda (Cache) geçici bir dosya oluşturuyoruz
            File tempFile = File.createTempFile("temp_video", ".mp4", getCacheDir());
            tempFile.deleteOnExit(); // Uygulama kapanınca bu dosya silinsin (yer kaplamasın)

            // Seçilen videonun içindeki veriyi (bytes) okumak için tünel açıyoruz
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

            // Veriyi tünelden geçirip kendi dosyamıza yazıyoruz
            byte[] buffer = new byte[4096]; // 4 KB'lık paketler halinde taşı
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // Kopyalama bitti, artık elimizde taş gibi %100 gerçek bir dosya yolu var
            filePath = tempFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("FILE_PICKER", "Dosya kopyalanırken hata oluştu: " + e.getMessage());
        }

        return filePath;
    }
    // 4. FFmpeg ile Dönüştürme İşlemini Başlatan Metod
    private void startConversion(String inputPath) {
        statusText.setText("Durum: Dönüştürme Başlıyor...");

        // Çıkış dosyası Download klasörüne 'donusturulen_ses.mp3' olarak gidecek
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String outputPath = new File(downloadDir, "donusturulen_ses.mp3").getAbsolutePath();

        // Komut: Giriş ve çıkış yollarını boşluklara karşı tırnak içine alıyoruz
        String command = "-y -i \"" + inputPath + "\" \"" + outputPath + "\"";

        FFmpegKit.executeAsync(command, session -> {
            if (ReturnCode.isSuccess(session.getReturnCode())) {
                runOnUiThread(() -> statusText.setText("Dönüştürme Başarılı! ✅\nKaydedildi: Download/donusturulen_ses.mp3"));
            } else {
                runOnUiThread(() -> {
                    statusText.setText("Hata! Dönüştürülemedi.");
                    Log.e("FFMPEG", session.getFailStackTrace());
                });
            }
        });
    }
}