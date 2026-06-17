package com.attentionmirror.tracking

import android.service.quicksettings.TileService
import android.widget.Toast
import com.attentionmirror.data.AttentionRepository
import com.attentionmirror.domain.DefaultPlatforms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Quick Settings tile: while scrolling Instagram/YouTube/etc. the user pulls
 * down and taps "I saw an ad". We attribute the mark to whatever tracked app is
 * currently foreground (via usage events) and feed it into calibration. No
 * overlay permission, no screen reading. See [com.attentionmirror.domain.Calibration].
 */
class AdMarkTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repo = AttentionRepository.create(applicationContext)
            val pkg = repo.currentTrackedPackage()
            val message = if (pkg != null) {
                repo.markAd(pkg)
                val name = DefaultPlatforms.BY_PACKAGE[pkg]?.platform ?: pkg
                "Ad logged for $name"
            } else {
                "Open a tracked app first, then mark the ad"
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
