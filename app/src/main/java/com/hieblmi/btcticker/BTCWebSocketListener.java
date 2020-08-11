package com.hieblmi.btcticker;

import android.app.Activity;
import android.content.res.Resources;
import android.util.Log;

import com.google.gson.Gson;
import com.hieblmi.btcticker.databinding.ActivityBtctickerBinding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class BTCWebSocketListener extends WebSocketListener {

    private final String TAG = this.getClass().getName();
    private Activity mActivity;
    private ActivityBtctickerBinding mBinding;
    private Queue mBuyQueue;
    private Queue mSellQueue;

    public BTCWebSocketListener(BTCTickerActivity activity, ActivityBtctickerBinding binding, PriorityBlockingQueue<TickerUpdate> buyQueue, PriorityBlockingQueue<TickerUpdate> sellQueue) {
        super();
        this.mActivity = activity;
        this.mBinding = binding;
        this.mBuyQueue = buyQueue;
        this.mSellQueue = sellQueue;
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosing(webSocket, code, reason);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String response) {
        super.onMessage(webSocket, response);

        TickerUpdate update = new Gson().fromJson(response, TickerUpdate.class);
        Log.d(TAG, update.toString());

        if (!"ticker".equals(update.getType()))
            return;

        formatTickerData(update);

        if ("buy".equals(update.getSide()))
            mBuyQueue.add(update);
        else
            mSellQueue.add(update);
    }

    private void formatTickerData(TickerUpdate update) {
        update.setPrice(String.format("%.2f", Double.parseDouble(update.getPrice())));
        update.setSize(String.format("%.4f", Double.parseDouble(update.getSize())));
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
        try {
            Resources res = mActivity.getResources();
            InputStream in_s = res.openRawResource(R.raw.matches);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            webSocket.send(new String(b));
        } catch (Exception e) {
            Log.d(TAG, "onOpen");
            e.printStackTrace();
        }
    }
}
