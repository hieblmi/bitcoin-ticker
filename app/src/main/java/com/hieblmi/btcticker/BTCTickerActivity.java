package com.hieblmi.btcticker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hieblmi.btcticker.databinding.ActivityBtctickerBinding;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocketListener;

public class BTCTickerActivity extends AppCompatActivity {

    private final String BITCOIN = " \u20BF ";
    private final String DOLLAR = "$";
    private final String mUrl = "wss://ws-feed.pro.coinbase.com";
    private final int ANIMATION_DURATION = 6000;
    private final int BACKGROUND_COLOR = 0xFF070F17;

    RelativeLayout.LayoutParams layoutParams;

    private ActivityBtctickerBinding mBinding;
    private WebSocketListener mWebSocketListener;
    private OkHttpClient mHttpClient;

    private PriorityBlockingQueue<TickerUpdate> buyQueue;
    private PriorityBlockingQueue<TickerUpdate> sellQueue;
    Timer timer;
    TimerTask updateUITaskBuys;
    TimerTask updateUITaskSells;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(BACKGROUND_COLOR);

        buyQueue = new PriorityBlockingQueue<>();
        sellQueue = new PriorityBlockingQueue<>();
        mWebSocketListener = new BTCWebSocketListener(this, mBinding, buyQueue, sellQueue);
    }

    private void startTicker() {
        mHttpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        setContentView(R.layout.activity_btcticker);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_btcticker);
        subscribe(mHttpClient, mWebSocketListener);
        startUpdateUITimer();
    }

    private void subscribe(OkHttpClient client, WebSocketListener listener) {
        Request request = new Request.Builder().url(mUrl).build();
        client.newWebSocket(request, listener);
        client.dispatcher().executorService().shutdown();
    }

    private void stopTicker() {
        updateUITaskBuys.cancel();
        updateUITaskSells.cancel();
        mHttpClient.dispatcher().cancelAll();
        mBinding.mainView.removeAllViews();
    }

    private void startUpdateUITimer() {
        timer = new Timer();
        updateUITaskBuys = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (buyQueue.isEmpty()) {
                        return;
                    }
                    animate(getListView(buyQueue), "buy");
                });
            }
        };
        updateUITaskSells = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (sellQueue.isEmpty()) {
                        return;
                    }
                    animate(getListView(sellQueue), "sell");
                });
            }
        };
        timer.scheduleAtFixedRate(updateUITaskBuys, 0, 400);
        timer.scheduleAtFixedRate(updateUITaskSells, 0, 400);
    }

    private void animate(ListView listView, String side) {
        ObjectAnimator backgroundFade = backgroundFade(listView, side);
        ObjectAnimator translationAnimation = getViewTranslationAnimation(listView, side);
        translationAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
        translationAnimation.setDuration(ANIMATION_DURATION);
        AnimatorSet animation = new AnimatorSet();
        animation.play(translationAnimation).with(backgroundFade);
        final ListView v = listView;
        animation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                v.setVisibility(View.INVISIBLE);
            }
        });
        animation.start();
    }

    @NotNull
    private ListView getListView(PriorityBlockingQueue<TickerUpdate> queue) {
        ListView list = new ListView(this);
        List<TickerUpdate> updates = new ArrayList<>();
        queue.drainTo(updates);
        ArrayAdapter<TickerUpdate> adapter = new TickerArrayAdapter<>(
                this,
                R.layout.ticker_view,
                R.id.priceView);
        adapter.addAll(updates);
        list.setAdapter(adapter);

        layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        mBinding.mainView.addView(list, layoutParams);
        return list;
    }

    private ObjectAnimator backgroundFade(ListView view, String side) {
        ObjectAnimator colorFade = ObjectAnimator.ofObject(
                view,
                "backgroundColor",
                new ArgbEvaluator(),
                "buy".equals(side) ? 0x6600FF00 : 0x66FF0000,
                BACKGROUND_COLOR);
        colorFade.setDuration(100);
        colorFade.setStartDelay(0);
        return colorFade;
    }

    private ObjectAnimator getViewTranslationAnimation(View view, String side) {
        PropertyValuesHolder y = PropertyValuesHolder.ofFloat("translationY", 1000f);
        if ("buy".equals(side))
            y = PropertyValuesHolder.ofFloat("translationY", -1000f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.0f);

        return ObjectAnimator.ofPropertyValuesHolder(view, alpha, y);
    }

    private class TickerArrayAdapter<T extends TickerUpdate> extends ArrayAdapter<T> {

        public TickerArrayAdapter(@NonNull Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            TextView textView = view.findViewById(R.id.priceView);

            TickerUpdate u = getItem(position);
            if ("buy".equals(u.getSide()))
                textView.setTextColor(Color.GREEN);
            else
                textView.setTextColor(Color.RED);

            textView.setText(String.format("%s %s %s%s", u.getSize(), BITCOIN, DOLLAR, u.getPrice()));

            return view;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTicker();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startTicker();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopTicker();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
}
