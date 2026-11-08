package app.velocity.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

/** Recycles emoji cells; never creates thousands of Buttons or keyboards. */
public final class EmojiPanel extends LinearLayout {

  public interface Listener {
    void selected(String emoji);
    void search();
  }

  private final Listener listener;
  private EmojiRepository.Catalog catalog;
  private final RecyclerView grid;
  private final TextView summary;
  private final LinearLayout categories;
  private final EmojiAdapter adapter = new EmojiAdapter();
  private final List<EmojiRepository.Entry> matches = new ArrayList<>();
  private String category = "all",
    tone = "all",
    query = "";
  private boolean detached;

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  public EmojiPanel(Context c, Listener listener, String initialQuery) {
    super(c);
    this.listener = listener;
    query = initialQuery;
    setOrientation(VERTICAL);
    setBackgroundColor(0xfff3f6fc);
    LinearLayout filters = new LinearLayout(c);
    Button search = new Button(c);
    search.setAllCaps(false);
    search.setText(query.isEmpty() ? "⌕ Buscar emojis" : query);
    search.setTextSize(12);
    search.setContentDescription("Buscar emojis en español");
    search.setOnClickListener(v -> listener.search());
    filters.addView(search, new LayoutParams(0, dp(44), 1));
    Spinner tones = new Spinner(c);
    String[] labels = {
      "Todos los tonos",
      "Sin modificador",
      "🏻 Claro",
      "🏼 Claro medio",
      "🏽 Medio",
      "🏾 Oscuro medio",
      "🏿 Oscuro",
      "Combinados",
    };
    String[] values = { "all", "0", "1", "2", "3", "4", "5", "mixed" };
    tones.setAdapter(
      new ArrayAdapter<>(
        c,
        android.R.layout.simple_spinner_dropdown_item,
        labels
      )
    );
    tones.setContentDescription("Tono de piel");
    filters.addView(tones, new LayoutParams(dp(145), dp(44)));
    tones.setOnItemSelectedListener(
      new AdapterView.OnItemSelectedListener() {
        public void onItemSelected(AdapterView<?> a, View v, int pos, long id) {
          tone = values[pos];
          filter();
        }

        public void onNothingSelected(AdapterView<?> a) {}
      }
    );
    addView(filters);
    HorizontalScrollView scroll = new HorizontalScrollView(c);
    scroll.setHorizontalScrollBarEnabled(false);
    categories = new LinearLayout(c);
    scroll.addView(categories);
    addView(scroll, new LayoutParams(-1, dp(42)));
    summary = new TextView(c);
    summary.setText("Cargando catálogo local…");
    summary.setTextSize(10);
    summary.setTextColor(0xff657692);
    summary.setPadding(dp(10), dp(4), dp(10), dp(4));
    addView(summary);
    grid = new RecyclerView(c);
    grid.setLayoutManager(
      new GridLayoutManager(
        c,
        Math.max(
          5,
          (int) (getResources().getDisplayMetrics().widthPixels /
            getResources().getDisplayMetrics().density /
            45)
        )
      )
    );
    grid.setAdapter(adapter);
    grid.setItemAnimator(null);
    addView(grid, new LayoutParams(-1, dp(145)));
    EmojiRepository.load(
      c,
      new EmojiRepository.Callback() {
        public void loaded(EmojiRepository.Catalog data) {
          if (detached) return;
          catalog = data;
          categoryButton("all", "∞ Todos");
          categoryButton("recent", "◷ Recientes");
          categoryButton("favorites", "★ Favoritos");
          for (EmojiRepository.Category group : data.categories)
            categoryButton(group.id, group.icon + " " + group.name);
          filter();
        }

        public void failed() {
          if (!detached) summary.setText(
            "No se pudo cargar. Vuelve a abrir el panel."
          );
        }
      }
    );
  }

  private void categoryButton(String id, String label) {
    Button button = new Button(getContext());
    button.setText(label);
    button.setTextSize(10);
    button.setAllCaps(false);
    button.setOnClickListener(v -> {
      category = id;
      filter();
    });
    categories.addView(button, new LayoutParams(-2, -1));
  }

  private void filter() {
    if (catalog == null) return;
    matches.clear();
    List<String> ids = "recent".equals(category)
      ? EmojiRepository.ids(getContext(), "emojiRecent")
      : "favorites".equals(category)
        ? EmojiRepository.ids(getContext(), "emojiFavorites")
        : null;
    String[] words = EmojiRepository.normalize(query).split("\\s+");
    for (EmojiRepository.Entry entry : catalog.entries) {
      if (
        !"all".equals(category) &&
        !(ids != null ? ids.contains(entry.id) : entry.group.equals(category))
      ) continue;
      if (!"all".equals(tone) && !tone.equals(entry.tone)) continue;
      boolean match = true;
      for (String word : words)
        if (!entry.search.contains(word)) {
          match = false;
          break;
        }
      if (match) matches.add(entry);
    }
    if (ids != null) Collections.sort(matches, (a, b) ->
      Integer.compare(ids.indexOf(a.id), ids.indexOf(b.id))
    );
    summary.setText(
      matches.size() +
        " resultados · Unicode 17.0 · mantener para favorito/variantes"
    );
    adapter.notifyDataSetChanged();
    grid.scrollToPosition(0);
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    ((GridLayoutManager) grid.getLayoutManager()).setSpanCount(
      Math.max(4, w / dp(44))
    );
  }

  @Override
  protected void onDetachedFromWindow() {
    detached = true;
    super.onDetachedFromWindow();
  }

  private final class Cell extends RecyclerView.ViewHolder {

    final TextView text;

    Cell(TextView text) {
      super(text);
      this.text = text;
    }
  }

  private final class EmojiAdapter extends RecyclerView.Adapter<Cell> {

    @Override
    public Cell onCreateViewHolder(ViewGroup parent, int type) {
      TextView text = new TextView(getContext());
      text.setTextSize(25);
      text.setGravity(Gravity.CENTER);
      text.setTextColor(Color.BLACK);
      text.setBackgroundResource(android.R.drawable.list_selector_background);
      text.setLayoutParams(new ViewGroup.LayoutParams(-1, dp(46)));
      return new Cell(text);
    }

    @Override
    public void onBindViewHolder(Cell holder, int position) {
      EmojiRepository.Entry entry = matches.get(position);
      holder.text.setText(entry.emoji);
      holder.text.setContentDescription(entry.emoji);
      holder.text.setOnClickListener(v -> {
        EmojiRepository.recent(getContext(), entry, false);
        listener.selected(entry.emoji);
      });
      holder.text.setOnLongClickListener(v -> {
        PopupMenu menu = new PopupMenu(getContext(), holder.text);
        menu
          .getMenu()
          .add("★ Guardar / quitar favorito")
          .setOnMenuItemClickListener(item -> {
            boolean added = EmojiRepository.favorite(getContext(), entry);
            Toast.makeText(
              getContext(),
              added ? "Favorito guardado" : "Favorito eliminado",
              Toast.LENGTH_SHORT
            ).show();
            if ("favorites".equals(category)) filter();
            return true;
          });
        String base =
          entry.parent.isEmpty() || "null".equals(entry.parent)
            ? entry.id
            : entry.parent;
        for (EmojiRepository.Entry variant : catalog.entries)
          if (base.equals(variant.parent) || base.equals(variant.id)) menu
            .getMenu()
            .add(variant.emoji + " · " + variant.label)
            .setOnMenuItemClickListener(item -> {
              EmojiRepository.recent(getContext(), variant, false);
              listener.selected(variant.emoji);
              return true;
            });
        menu.show();
        return true;
      });
    }

    @Override
    public int getItemCount() {
      return matches.size();
    }
  }
}
