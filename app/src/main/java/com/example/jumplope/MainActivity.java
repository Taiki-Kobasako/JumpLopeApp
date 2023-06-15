package com.example.jumplope;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //以下最初の画面の要素
    private TextView countTextView;
    private TextView timeTextView;
    private TextView statusTextView;
    private TextView count_guide_TextView;
    private TextView time_guide_TextView;
    private EditText targetCountEditText;
    private EditText timeLimitEditText;
    private EditText weightEditText;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;

    //以下結果画面の要素
    private TextView result_TextView;
    private TextView result_goalTextView;
    private TextView result_goalTextView2;
    private TextView result_countTextView;
    private TextView result_countTextView2;
    private TextView result_timeTextView;
    private TextView result_timeTextView2;
    private TextView result_resultTextView;
    private TextView result_resultTextView2;
    private TextView result_kcalTextView;
    private TextView result_kcalTextView2;
    private Button closeButton;

    private int totalCount;
    private int timeLimit;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning;
    private boolean isCounting;
    private boolean isPaused;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float[] gravity;
    private boolean isJumping;
    long countMillis=0;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //デフォルトの画面要素
        countTextView = findViewById(R.id.countTextView);
        timeTextView = findViewById(R.id.timerTextView);
        count_guide_TextView = findViewById(R.id.count_guide_textView);
        time_guide_TextView = findViewById(R.id.time_guide_textView);
        statusTextView = findViewById(R.id.status);
        targetCountEditText = findViewById(R.id.targetCountEditText);
        timeLimitEditText = findViewById(R.id.timeLimitEditText);
        weightEditText = findViewById(R.id.weightEditText);
        startButton = findViewById(R.id.StartButton);
        stopButton = findViewById(R.id.StopButton);
        resetButton = findViewById(R.id.ResetButton);

        //結果の画面要素
        result_TextView = findViewById(R.id.result_textView);
        result_goalTextView = findViewById(R.id.goal_textView);
        result_goalTextView2 = findViewById(R.id.goal_textView2);
        result_countTextView = findViewById(R.id.result_count_textView);
        result_countTextView2 = findViewById(R.id.result_count_textView2);
        result_timeTextView = findViewById(R.id.result_time_textView);
        result_timeTextView2 = findViewById(R.id.result_time_textView2);
        result_resultTextView = findViewById(R.id.result_result_textView);
        result_resultTextView2 = findViewById(R.id.result_result_textView2);
        result_kcalTextView =  findViewById(R.id.result_kcal_textView);
        result_kcalTextView2 =  findViewById(R.id.result_kcal_textView2);
        closeButton = findViewById(R.id.close_button);

        //結果画面の非表示化
        result_TextView.setVisibility(View.INVISIBLE);
        result_goalTextView.setVisibility(View.INVISIBLE);
        result_goalTextView2.setVisibility(View.INVISIBLE);
        result_countTextView.setVisibility(View.INVISIBLE);
        result_countTextView2.setVisibility(View.INVISIBLE);
        result_timeTextView.setVisibility(View.INVISIBLE);
        result_timeTextView2.setVisibility(View.INVISIBLE);
        result_resultTextView.setVisibility(View.INVISIBLE);
        result_resultTextView2.setVisibility(View.INVISIBLE);
        result_kcalTextView.setVisibility(View.INVISIBLE);
        result_kcalTextView2.setVisibility(View.INVISIBLE);
        closeButton.setVisibility(View.INVISIBLE);

        stopButton.setEnabled(false);

        //スタートボタンのクリックイベントの設定
        startButton.setOnClickListener(v -> startCounting());

        //ストップボタンのクリックイベントの設定
        stopButton.setOnClickListener(v -> pauseCounting());

        //リセットボタンのクリックイベントの設定
        resetButton.setOnClickListener(v -> resetCount());

        //閉じるボタンのクリックイベントの設定
        closeButton.setOnClickListener(v -> closeResult());

        //センサーマネージャの作成・登録
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gravity = new float[3];

        //ジャンプフラッグの無効か
        isJumping = false;

        //通知の設定
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

    }

    @Override
    protected void onResume(){
        super.onResume();
        //センサーリスナーの登録
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause(){
        super.onPause();
        //センサーリスナーの解除
        sensorManager.unregisterListener(this);
    }

    //加速度センサーが変化したとき
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float alpha = 0.8f;
            float jump_threshold = 15.0f;

            //重力値を取得(振動・ノイズ対策ローパスフィルタ)
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];

            //重力成分の除去
            float accelerationY = event.values[1] - gravity[1];

            // ジャンプ中かどうか判定
            if (isJumping) {
                // ジャンプ中において加速度が閾値以下を下回ったら
                if (accelerationY <= jump_threshold) {
                    //ジャンプ終了としジャンプカウントする
                    isJumping = false;
                    countJump();
                }
            } else {
                // ジャンプ中でない状態で上向きの加速度が閾値を超えたとき）
                if (accelerationY > jump_threshold) {
                    // ジャンプ中と判断
                    isJumping = true;
                }
            }
        }
    }


    //必須なので
    public void onAccuracyChanged(Sensor sensor, int accuracy){
        //なし
    }

    //ジャンプのカウント処理
    private void countJump() {
        // カウント中でありかつ一時停止中でないことの確認
        if (isCounting && !isPaused) {
            //カウントを足す
            totalCount++;
            //カウンターの表示を更新
            countTextView.setText(String.valueOf(totalCount));
        }
    }

    private void startCounting(){
        //カウント中でないか確認
        if(!isCounting){
            //以下カウントを開始するときの処理
            //数値が入力されているか確認
            if(validateInput()){
                //カウンターの初期化
                totalCount = 0;
                //制限時間の取得(ミリ秒で代入)
                timeLimit = Integer.parseInt(timeLimitEditText.getText().toString()) * 1000;


                countDownTimer = new CountDownTimer(timeLimit, 1000) {
                    // インターバル(countDownInterval)毎に呼ばれ実行する
                    @Override
                    public void onTick(long millisUntilFinished) {
                        updateTimer(millisUntilFinished);
                        countMillis = millisUntilFinished;
                    }

                    // カウントダウン終了時の処理
                    @Override
                    public void onFinish() {
                        finishCounting();
                    }
                };

                //誤操作防止処理
                statusTextView.setText("");
                countDownTimer.start();
                isTimerRunning = true;
                isCounting = true;
                isPaused = false;
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                resetButton.setEnabled(false);
                targetCountEditText.setEnabled(false);
                timeLimitEditText.setEnabled(false);
                weightEditText.setEnabled(false);
            }
        }else{
            //一時停止中なら実行
            if(isPaused){
                resumeCounting();
                statusTextView.setText("");
            }else{
                //ありえないが一応
                Toast.makeText(this,"すでに始まっています",Toast.LENGTH_SHORT).show();
            }
        }
    }

    //一時停止の処理
    private void pauseCounting(){
        if(isCounting && isTimerRunning && !isPaused ){
            //カウントダウンタイマー
            countDownTimer.cancel();

            //一時停止の処理
            isTimerRunning = false;
            isPaused = true;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            resetButton.setEnabled(true);
            statusTextView.setText("一時停止中");
        }
    }

    //一時停止から再開する用
    private void resumeCounting() {
        // フラグの確認(カウントが開始していて、タイマーが動いておらず、一時停止しているか)
        if (isCounting && !isTimerRunning && isPaused) {
            countDownTimer = new CountDownTimer(countMillis, 100) {

                // インターバル毎に行われる処理
                @Override
                public void onTick(long millisUntilFinished) {
                    updateTimer(millisUntilFinished);
                    countMillis = millisUntilFinished;
                }

                // カウントが終了したら
                @Override
                public void onFinish() {
                    finishCounting();
                }
            };

            countDownTimer.start();
            isTimerRunning = true;
            isPaused = false;
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            resetButton.setEnabled(false);
        }
    }

    //カウント終了時
    private  void finishCounting(){
        isTimerRunning = false;
        isCounting = false;
        isPaused = false;

        //結果の表示およびユーザへの通知
        result_show();
        vibrate();
        playRingtone();
    }

    private void resetCount(){
        targetCountEditText.setEnabled(true);
        timeLimitEditText.setEnabled(true);
        weightEditText.setEnabled(true);
        targetCountEditText.setText("");
        timeLimitEditText.setText("");
        weightEditText.setText("");
        countTextView.setText("0");
        statusTextView.setText("");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        resetButton.setEnabled(true);
        isTimerRunning = false;
        isCounting = false;
        isPaused = false;
        totalCount = 0;
        updateTimer(0);
    }

    private void closeResult(){
        //カウントのリセットおよびデフォルトの画面構成に戻す
        resetCount();
        default_elements_show();
    }

    @SuppressLint("DefaultLocale")
    private void result_show(){

        int totalCount = this.totalCount;
        double weight = Double.parseDouble(weightEditText.getText().toString());
        result_elements_show();
        int targetCount  = Integer.parseInt(targetCountEditText.getText().toString());
        int timeLimit = Integer.parseInt(timeLimitEditText.getText().toString());
        int elapsedMinutes = Math.toIntExact((timeLimit - (countMillis /1000)) / 60);
        int elapsedSeconds = Math.toIntExact((timeLimit - (countMillis /1000)) % 60);

        result_goalTextView2.setText(String.valueOf(Integer.parseInt(targetCountEditText.getText().toString())));
        result_goalTextView2.setText(String.valueOf(targetCount));
        result_countTextView2.setText(String.valueOf(totalCount));
        result_timeTextView2.setText(String.format("%02d:%02d", elapsedMinutes, elapsedSeconds));
        if(totalCount >= targetCount){
            result_resultTextView2.setText("目標達成");
        }else{
            result_resultTextView2.setText("目標未達成");
        }
        result_kcalTextView2.setText(calculateCalories(totalCount, weight, Math.toIntExact(timeLimit-(countMillis / 1000))));
    }

    //カロリー計算
    @SuppressLint("DefaultLocale")
    private String calculateCalories(int count, double weight, int time) {
        double jumpsPerMinute = count / (time / 60.0);
        double METs,totalCalories;

        //METs算出
        if (jumpsPerMinute >= 120) {
            METs = 12.3;
        } else if (jumpsPerMinute >= 100) {
            METs = 11.8;
        } else {
            METs = 8.8;
        }

        //さぼり検出
        if(count == 0){
            totalCalories = 0;
        }else{
            totalCalories = METs * weight * time * 1.05 / 60.0;

        }

        //カロリー出力
        return String.format("%.1f", totalCalories);
    }

    //結果画面準備
    private void result_elements_show(){
        //デフォルト画面の表示
        countTextView.setVisibility(View.INVISIBLE);
        timeTextView.setVisibility(View.INVISIBLE);
        count_guide_TextView.setVisibility(View.INVISIBLE);
        time_guide_TextView.setVisibility(View.INVISIBLE);
        statusTextView.setVisibility(View.INVISIBLE);
        targetCountEditText.setVisibility(View.INVISIBLE);
        timeLimitEditText.setVisibility(View.INVISIBLE);
        weightEditText.setVisibility(View.INVISIBLE);
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.INVISIBLE);
        resetButton.setVisibility(View.INVISIBLE);

        //結果の非表示
        result_TextView.setVisibility(View.VISIBLE);
        result_goalTextView.setVisibility(View.VISIBLE);
        result_goalTextView2.setVisibility(View.VISIBLE);
        result_countTextView.setVisibility(View.VISIBLE);
        result_countTextView2.setVisibility(View.VISIBLE);
        result_timeTextView.setVisibility(View.VISIBLE);
        result_timeTextView2.setVisibility(View.VISIBLE);
        result_resultTextView.setVisibility(View.VISIBLE);
        result_resultTextView2.setVisibility(View.VISIBLE);
        result_kcalTextView.setVisibility(View.VISIBLE);
        result_kcalTextView2.setVisibility(View.VISIBLE);
        closeButton.setVisibility(View.VISIBLE);
    }

    //デフォルト画面のみの表示関数
    private void default_elements_show(){
        //デフォルト画面の表示
        countTextView.setVisibility(View.VISIBLE);
        timeTextView.setVisibility(View.VISIBLE);
        count_guide_TextView.setVisibility(View.VISIBLE);
        time_guide_TextView.setVisibility(View.VISIBLE);
        statusTextView.setVisibility(View.VISIBLE);
        targetCountEditText.setVisibility(View.VISIBLE);
        timeLimitEditText.setVisibility(View.VISIBLE);
        weightEditText.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        resetButton.setVisibility(View.VISIBLE);

        //結果の非表示
        result_TextView.setVisibility(View.INVISIBLE);
        result_goalTextView.setVisibility(View.INVISIBLE);
        result_goalTextView2.setVisibility(View.INVISIBLE);
        result_countTextView.setVisibility(View.INVISIBLE);
        result_countTextView2.setVisibility(View.INVISIBLE);
        result_timeTextView.setVisibility(View.INVISIBLE);
        result_timeTextView2.setVisibility(View.INVISIBLE);
        result_resultTextView.setVisibility(View.INVISIBLE);
        result_resultTextView2.setVisibility(View.INVISIBLE);
        result_kcalTextView.setVisibility(View.INVISIBLE);
        result_kcalTextView2.setVisibility(View.INVISIBLE);
        closeButton.setVisibility(View.INVISIBLE);
    }

    //タイマーのアップデート
    private void updateTimer(long millisUntilFinished){
        int seconds = (int)(millisUntilFinished / 1000);
        @SuppressLint("DefaultLocale") String time = String.format("%02d:%02d", seconds/60, seconds%60);
        timeTextView.setText(time);
    }

    //振動させる
    private void vibrate(){
        long[] pattern = {0, 1000, 500, 1000, 500, 1000};
        if(vibrator.hasVibrator()){
            vibrator.vibrate(pattern, -1);
        }
    }

    //音を鳴らす
    // サウンド再生
    private void playRingtone() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.end);
        // メディアプレーヤーのリソースを解放
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            mp.release(); // エラーが発生した場合もリソースを解放
            return false;
        });
        mediaPlayer.start();
    }

    //フォーム入力の確認
    private boolean validateInput(){
        String targetCountString = targetCountEditText.getText().toString();
        String timeLimitString = timeLimitEditText.getText().toString();
        String weightString = weightEditText.getText().toString().trim();

        //入力されているか確認
        if(targetCountString.isEmpty() || timeLimitString.isEmpty() || weightString.isEmpty()){
            Toast.makeText(this, "空欄があります", Toast.LENGTH_SHORT).show();
            return false;
        }

        //入力された値が正しいか確認
        int target = Integer.parseInt(targetCountString);
        int time = Integer.parseInt(timeLimitString);
        double weight = Double.parseDouble((weightString));

        if(target <= 0 || time <= 0 || weight <= 0){
            Toast.makeText(this, "正しい値を入力してください", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

}
