package tv.mta.flutter_playout

import android.util.Log
import io.flutter.plugin.common.PluginRegistry
import tv.mta.flutter_playout.audio.AudioPlayer
import tv.mta.flutter_playout.video.PlayerViewFactory

class FlutterPlayoutPlugin {
  companion object {
    @JvmStatic
    fun registerWith(registrar: PluginRegistry.Registrar) {
      PlayerViewFactory.registerWith(registrar)
      AudioPlayer.registerWith(registrar)
    }
  }
}
