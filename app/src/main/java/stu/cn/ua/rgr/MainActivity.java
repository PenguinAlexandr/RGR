package stu.cn.ua.rgr;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView sensorData;
    private FrameLayout frameLayout;
    private boolean isTextVisible = true;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorData = findViewById(R.id.sensorData);
        frameLayout = findViewById(R.id.frameLayout);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                sensorData.setText("Акселерометр не доступний");
            }
        } else {
            sensorData.setText("Диспетчер датчиків недоступний");
        }

        // Встановлення початкового кольору фону та кольору тексту
        updateColors();

        // Налаштування слухача кліків для кореневої розкладки
        findViewById(R.id.rootLayout).setOnClickListener(v -> toggleTextVisibility());

        // Налаштування прослуховування кліків для текстового подання
        sensorData.setOnClickListener(v -> copyBackgroundColorToClipboard());

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void toggleTextVisibility() {
        isTextVisible = !isTextVisible;
        if (isTextVisible) {
            sensorData.setVisibility(View.VISIBLE);
        } else {
            sensorData.setVisibility(View.INVISIBLE);
        }
    }

    private void copyBackgroundColorToClipboard() {
        int backgroundColor = ((ColorDrawable) frameLayout.getBackground()).getColor();
        String hexColor = String.format("#%06X", (0xFFFFFF & backgroundColor));
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Колір", hexColor);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            showToast("Колір скопійовано у буфер обміну: " + hexColor);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = roundToThousandths(event.values[0]);
            float y = roundToThousandths(event.values[1]);
            float z = roundToThousandths(event.values[2]);

            // Оновлення тексту значеннями датчиків та кольором фону
            String sensorText = "X: " + x + "\nY: " + y + "\nZ: " + z + "\nColor: #" +
                    Integer.toHexString(((ColorDrawable) frameLayout.getBackground()).getColor());
            sensorData.setText(sensorText);

            // Оновлення кольорів на основі значень датчиків з плавним переходом
            updateColorsSmoothly(x, y, z);
        }
    }

    private float roundToThousandths(float value) {
        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        String formattedValue = decimalFormat.format(value).replace(",", ".");
        return Float.parseFloat(formattedValue);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Не використовується, але необхідний для реалізації SensorEventListener
    }

    private void updateColors() {
        int backgroundColor = Color.rgb((int) (Math.abs((float) 0) * 255 / 10),
                (int) (Math.abs((float) 0) * 255 / 10), (int) (Math.abs((float) 0) * 255 / 10));

        int textColor = Color.rgb(255 - Color.red(backgroundColor),
                255 - Color.green(backgroundColor), 255 - Color.blue(backgroundColor));

        frameLayout.setBackgroundColor(backgroundColor);
        sensorData.setTextColor(textColor);
    }

    private void updateColorsSmoothly(float x, float y, float z) {
        int targetBackgroundColor = Color.rgb((int) (Math.abs(x) * 255 / 10),
                (int) (Math.abs(y) * 255 / 10), (int) (Math.abs(z) * 255 / 10));

        int targetTextColor = Color.rgb(255 - Color.red(targetBackgroundColor),
                255 - Color.green(targetBackgroundColor), 255 - Color.blue(targetBackgroundColor));

        int currentBackgroundColor = ((ColorDrawable) frameLayout.getBackground()).getColor();
        int currentTextColor = sensorData.getCurrentTextColor();

        // Плавний перехід кольору фону
        ValueAnimator backgroundColorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                currentBackgroundColor, targetBackgroundColor);
        backgroundColorAnimator.addUpdateListener(animator -> frameLayout.setBackgroundColor((int) animator.getAnimatedValue()));

        // Плавний перехід кольору тексту
        ValueAnimator textColorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                currentTextColor, targetTextColor);
        textColorAnimator.addUpdateListener(animator -> sensorData.setTextColor((int) animator.getAnimatedValue()));

        // Тривалість та інтерполятор для більш плавної анімації
        int animationDuration = 80; // У мілісекундах
        backgroundColorAnimator.setDuration(animationDuration);
        textColorAnimator.setDuration(animationDuration);

        backgroundColorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        textColorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        // Старт анімації
        backgroundColorAnimator.start();
        textColorAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}