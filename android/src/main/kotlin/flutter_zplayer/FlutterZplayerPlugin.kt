package flutter_zplayer

import android.util.Log
import io.flutter.plugin.common.PluginRegistry
import flutter_zplayer.video.PlayerViewFactory

class FlutterZplayerPlugin {
  companion object {
    @JvmStatic
    fun registerWith(registrar: PluginRegistry.Registrar) {
      PlayerViewFactory.registerWith(registrar)
    }
  }
}
