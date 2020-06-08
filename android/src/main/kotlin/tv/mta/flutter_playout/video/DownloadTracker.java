package tv.mta.flutter_playout.video;/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.flutter.plugin.common.EventChannel;
import tv.mta.flutter_playout.R;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadCursor;
import com.google.android.exoplayer2.offline.DownloadHelper;
import com.google.android.exoplayer2.offline.DownloadIndex;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadRequest;
import tv.mta.flutter_playout.video.DownldService;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

/** Tracks media that has been downloaded. */
public class DownloadTracker{
    private EventChannel.EventSink eventSink;


    /** Listens for changes in the tracked downloads. */
    public interface Listener {

        /** Called when the tracked downloads changed. */
        void onDownloadsChanged();
    }

    private static final String TAG = "DownloadTracker";

    private final Context context;
    private final DataSource.Factory dataSourceFactory;
    private final CopyOnWriteArraySet<Listener> listeners;
    private final HashMap<Uri, Download> downloads;
    private  ProgressDialog trackProgressDialog;
    private Function dismissDialog;
    private ProgressDialog downloadProgressDialog;
    private final DownloadIndex downloadIndex;
    private  Activity activity;
    private final DefaultTrackSelector.Parameters trackSelectorParameters;
    Timer downTimer;
    DownloadManager downloadManager;
    @Nullable private StartDownloadDialogHelper startDownloadDialogHelper;

    public DownloadTracker(
            Context context, DataSource.Factory dataSourceFactory, DownloadManager downloadManager,Activity activity) {
        this.context = context.getApplicationContext();
        this.dataSourceFactory = dataSourceFactory;
        listeners = new CopyOnWriteArraySet<>();
        downloads = new HashMap<>();
        downloadIndex = downloadManager.getDownloadIndex();
        downTimer=new Timer("timerDownload");
        trackSelectorParameters = DownloadHelper.getDefaultTrackSelectorParameters(context);
        downloadManager.addListener(new DownloadManagerListener(activity,downloadProgressDialog));
        this.downloadManager=downloadManager;
        loadDownloads();
        downloadManager.addListener(
                new DownloadManager.Listener() {
                    @Override
                    public void onDownloadChanged(DownloadManager downloadManager, Download download) {
                       Log.e("hxbfhjd","dksjksdgfjg");
                        if (download.state == Download.STATE_DOWNLOADING) {
                            Log.e("jbhfcgjfdsg ","kdsjhgdsf");

//                            if(!downloadProgressDialog.isShowing()) {
//                                TimerTask _timerTask = new TimerTask() {
//                                    @Override
//                                    public void run() {
//                                        if (downloadProgressDialog != null && downloadProgressDialog.isShowing()) {
//                                            int percent = (int) download.getPercentDownloaded();
//                                            downloadProgressDialog.setProgress(percent);
//                                        }
//                                    }
//                                };
//                                downTimer.schedule(_timerTask,1000);
//                            }
                        }
                        if (download.state == Download.STATE_COMPLETED) {
                            Intent intent = new Intent("download_status");
                            intent.putExtra("status", "download_finished");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                        } else if (download.state == Download.STATE_FAILED) {
                            Intent intent = new Intent("download_status");
                            intent.putExtra("status", "download_failed");
                            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                        }

                    }

                });
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public boolean isDownloaded(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED;
    }

    public DownloadRequest getDownloadRequest(Uri uri) {
        Download download = downloads.get(uri);
        return download != null && download.state != Download.STATE_FAILED ? download.request : null;
    }
    public  void removeMedia( Uri uri,ProgressDialog progressDialog){
        Download download = downloads.get(uri);
        DownldService.sendRemoveDownload(
                context, DownldService.class, download.request.id, /* foreground= */ false);
                //Code hear
                progressDialog.dismiss();
                Intent intent = new Intent("download_status");
                intent.putExtra("status", "download_deleted");
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

    }
    public void toggleDownload(
            Activity activity,
            ProgressDialog trackProgressDialog,
            FragmentManager fragmentManager,
            String name,
            Uri uri,
            String extension,
            RenderersFactory renderersFactory,
            DialogInterface.OnDismissListener onDismissListener) {
        this.activity=activity;
        this.trackProgressDialog=trackProgressDialog;
        Download download = downloads.get(uri);
        if (download != null) {

            DownldService.sendRemoveDownload(
                    context, DownldService.class, download.request.id, /* foreground= */ false);
        } else {
            Log.e("kbndfbkdnf",":dnfknsd");
            if (startDownloadDialogHelper != null) {
                startDownloadDialogHelper.release();
            }

            startDownloadDialogHelper =
                    new StartDownloadDialogHelper(

                            fragmentManager, getDownloadHelper(uri, extension, renderersFactory), name,trackProgressDialog,onDismissListener);
        }
    }

    private void loadDownloads() {
        try (DownloadCursor loadedDownloads = downloadIndex.getDownloads()) {
            while (loadedDownloads.moveToNext()) {
                Download download = loadedDownloads.getDownload();
                downloads.put(download.request.uri, download);
            }
        } catch (IOException e) {
            Log.w(TAG, "Failed to query downloads", e);
        }
    }

    private DownloadHelper getDownloadHelper(
            Uri uri, String extension, RenderersFactory renderersFactory) {
                return DownloadHelper.forHls(context, uri, dataSourceFactory, renderersFactory);
    }

    private class DownloadManagerListener implements DownloadManager.Listener {
        Activity activity;
        ProgressDialog progress;

        public DownloadManagerListener(Activity activity,ProgressDialog progress) {
            this.activity = activity;
            this.progress=progress;
        }


        @Override
        public void onDownloadChanged(DownloadManager downloadManager, Download download) {
            downloads.put(download.request.uri, download);
            for (Listener listener : listeners) {
                listener.onDownloadsChanged();
            }
        }



        @Override
        public void onDownloadRemoved(DownloadManager downloadManager, Download download) {

            downloads.remove(download.request.uri);
            for (Listener listener : listeners) {
                listener.onDownloadsChanged();
            }

        }
    }

    private final class StartDownloadDialogHelper
            implements DownloadHelper.Callback,
            DialogInterface.OnClickListener,
            DialogInterface.OnDismissListener {

        private final FragmentManager fragmentManager;
        private final DownloadHelper downloadHelper;
        private final String name;
        DialogInterface.OnDismissListener onDismissListener;
        private TrackSelectionDialog trackSelectionDialog;
        private MappedTrackInfo mappedTrackInfo;
      private  ProgressDialog trackProgressDialog;
        public StartDownloadDialogHelper(
                FragmentManager fragmentManager, DownloadHelper downloadHelper, String name,ProgressDialog trackProgressDialog, DialogInterface.OnDismissListener onDismissListener) {
            this.fragmentManager = fragmentManager;
            this.downloadHelper = downloadHelper;
            this.onDismissListener=onDismissListener;
            this.name = name;
            this.trackProgressDialog=trackProgressDialog;
            downloadHelper.prepare(this);

        }

        public void release() {
            downloadHelper.release();
            if (trackSelectionDialog != null) {
                trackSelectionDialog.dismiss();
            }
        }

        // DownloadHelper.Callback implementation.

        @Override
        public void onPrepared(DownloadHelper helper) {

            if (helper.getPeriodCount() == 0) {
                Log.d(TAG, "No periods found. Downloading entire stream.");
                startDownload();
                downloadHelper.release();
                return;
            }
            mappedTrackInfo = downloadHelper.getMappedTrackInfo(/* periodIndex= */ 0);
            if (!TrackSelectionDialog.willHaveContent(mappedTrackInfo)) {
                Log.d(TAG, "No dialog content. Downloading entire stream.");
                startDownload();
                downloadHelper.release();
                return;
            }
            trackProgressDialog.dismiss();
            trackSelectionDialog =
                    TrackSelectionDialog.createForMappedTrackInfoAndParameters(
                            /* titleId= */ R.string.exo_download_description,
                            mappedTrackInfo,
                            trackSelectorParameters,
                            /* allowAdaptiveSelections =*/ false,
                            /* allowMultipleOverrides= */ true,
                            /* onClickListener= */ this,
                            /* onDismissListener= */  dismissedDialog-> {
                                Log.e("jdsbnfjdsnbjf","dmsbnfjsd");
                                onDismissListener.onDismiss(dismissedDialog);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    hideVirtualButtons();
                                }
                            });
            trackSelectionDialog.show(fragmentManager, /* tag= */ null);
        }
        @TargetApi(19)
        private void hideVirtualButtons() {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        @Override
        public void onPrepareError(DownloadHelper helper, IOException e) {
            Toast.makeText(context, R.string.download_start_error, Toast.LENGTH_LONG).show();
            Log.e(
                    TAG,
                    e instanceof DownloadHelper.LiveContentUnsupportedException
                            ? "Downloading live content unsupported"
                            : "Failed to start download",
                    e);
        }

        // DialogInterface.OnClickListener implementation.

        @Override
        public void onClick(DialogInterface dialog, int which) {
            for (int periodIndex = 0; periodIndex < downloadHelper.getPeriodCount(); periodIndex++) {
                downloadHelper.clearTrackSelections(periodIndex);
                for (int i = 0; i < mappedTrackInfo.getRendererCount(); i++) {
                    if (!trackSelectionDialog.getIsDisabled(/* rendererIndex= */ i)) {
                        Log.e("hjigbjdfngkjdf","DSgfdsf");
                        downloadHelper.addTrackSelectionForSingleRenderer(
                                periodIndex,
                                /* rendererIndex= */ i,
                                trackSelectorParameters,
                                trackSelectionDialog.getOverrides(/* rendererIndex= */ i));
                    }
                }
            }
            DownloadRequest downloadRequest = buildDownloadRequest();
            if (downloadRequest.streamKeys.isEmpty()) {
                // All tracks were deselected in the dialog. Don't start the download.
                return;
            }
            startDownload(downloadRequest);
        }

        // DialogInterface.OnDismissListener implementation.

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            trackSelectionDialog = null;
            downloadHelper.release();
        }

        // Internal methods.

        private void startDownload() {
            startDownload(buildDownloadRequest());
        }

        private void startDownload(DownloadRequest downloadRequest) {
            DownldService.sendAddDownload(
                    context, DownldService.class, downloadRequest, /* foreground= */ false);

            downloadProgressDialog = new ProgressDialog(activity);
            downloadProgressDialog.setMessage("Downloading...");
            downloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            downloadProgressDialog.setIndeterminate(true);

            new CountDownTimer(3000, 1000)
            {
                public void onTick(long l) {
                    downloadProgressDialog.show();

                }
                public void onFinish()
                {
                    //Code hear
                    downloadProgressDialog.dismiss();
                }
            }.start();
        }

        private DownloadRequest buildDownloadRequest() {

            return downloadHelper.getDownloadRequest(Util.getUtf8Bytes(name));

        }
    }
}
