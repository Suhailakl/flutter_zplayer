package flutter_zplayer.video;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;

import androidx.core.content.ContextCompat;
import io.flutter.Log;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.platform.PlatformView;
import flutter_zplayer.R;

public class PlayerView implements PlatformView, MethodChannel.MethodCallHandler {

    private final PlayerLayout player;

    PlayerView(Context context, Activity activity, int id, BinaryMessenger messenger, Object args) {

        new MethodChannel(messenger, "zplayer/NativeVideoPlayerMethodChannel_" + id)
                .setMethodCallHandler(this);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            activity.getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.bg_color));
//        }
//        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        player = new PlayerLayout(context, activity, messenger, id, args);
    }

    @Override
    public View getView() {
        return player;
    }


    @Override
    public void dispose() {
        player.onDestroy();
    }

    @Override
    public void onMethodCall(MethodCall call, @NotNull MethodChannel.Result result) {
        switch (call.method) {
            case "onMediaChanged":
                player.onMediaChanged(call.arguments);
                result.success(true);
                break;
            case "onShowControlsFlagChanged":
                player.onShowControlsFlagChanged(call.arguments);
                result.success(true);
                break;
            case "resume":
                player.play();
                result.success(true);
                break;
            case "pause":
                player.pause();
                result.success(true);
                break;
            case "setPreferredAudioLanguage":
                player.setPreferredAudioLanguage(call.arguments);
                result.success(true);
                break;
            case "seekTo":
                player.seekTo(call.arguments);
                result.success(true);
                break;
            case "removeMedia":
                player.removeMedia();
                result.success(true);
                break;
            case "trackSelectionPressed":
                player.onTrackSelectionPressed();
                result.success(true);
                break;
            case "downloadTrackSelectionPressed":
                player.onDownloadTrackSelectionPressed();
                result.success(true);
                break;
            case "dispose":
                dispose();
                result.success(true);
                break;
            default:
                result.notImplemented();
        }
    }
}