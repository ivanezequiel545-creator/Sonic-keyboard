package app.velocity.keyboard;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.TextView;
import androidx.core.graphics.ColorUtils;

/** Shared, contrast-aware colours for emoji and media panels; follows the user's theme. */
final class PanelColors {
  final int surface,ink,muted,accent,tile;
  PanelColors(Context context){
    int theme=KeyboardPreferences.themeIndex(context);
    int base=KeyboardPreferences.color(context,"backgroundColor",KeyboardPreferences.COLORS[theme]);
    surface=ColorUtils.blendARGB(base,0xffffffff,ColorUtils.calculateLuminance(base)<.4?.09f:.78f);
    ink=foreground(surface);int secondary=ColorUtils.blendARGB(ink,surface,.24f);muted=ColorUtils.calculateContrast(secondary,surface)>=4.5?secondary:ink;
    accent=KeyboardPreferences.color(context,"accentColor",KeyboardPreferences.ACCENTS[theme]);
    tile=ColorUtils.blendARGB(surface,accent,.13f);
  }
  static int foreground(int background){return ColorUtils.calculateLuminance(background)>.179?0xff000000:0xffffffff;}
  void chip(TextView view,boolean selected){
    int fill=selected?accent:tile;
    GradientDrawable shape=new GradientDrawable();shape.setColor(fill);shape.setCornerRadius(12*view.getResources().getDisplayMetrics().density);
    shape.setStroke(1,ColorUtils.blendARGB(fill,ink,.16f));
    view.setBackground(new RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(ink,40)),shape,null));
    view.setTextColor(foreground(fill));
  }
}
