package jp.panta.misskeyandroidclient.ui

import android.util.TypedValue
import androidx.cardview.widget.CardView
import androidx.databinding.BindingAdapter
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.R
import net.pantasystem.milktea.common.ColorUtil

object ViewBackgroundColorHelper {

    @BindingAdapter("setCardViewSurfaceColor")
    @JvmStatic
    fun CardView.setSurfaceColor(setCardViewSurfaceColor: Int?){
        if (setCardViewSurfaceColor != null) {
            this.setCardBackgroundColor(setCardViewSurfaceColor)
        }
        val cardView = this
        val miApp = cardView.context.applicationContext as MiApplication
        val store = miApp.colorSettingStore
        val typedValue = TypedValue()
        cardView.context.theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
        cardView.setCardBackgroundColor(ColorUtil.matchOpaqueAndColor(store.surfaceColorOpaque, typedValue.data))
    }
}