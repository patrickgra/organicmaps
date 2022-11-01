package app.organicmaps.bookmarks;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.recyclerview.widget.RecyclerView;

import app.organicmaps.R;
import app.organicmaps.adapter.OnItemClickListener;
import app.organicmaps.bookmarks.data.BookmarkCategory;
import app.organicmaps.bookmarks.data.BookmarkInfo;
import app.organicmaps.bookmarks.data.BookmarkManager;
import app.organicmaps.bookmarks.data.Track;
import app.organicmaps.location.LocationHelper;
import app.organicmaps.widget.recycler.RecyclerClickListener;
import app.organicmaps.widget.recycler.RecyclerLongClickListener;
import app.organicmaps.util.Graphics;
import app.organicmaps.util.UiUtils;

public class Holders
{
  public static class GeneralViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mText;
    @NonNull
    private final ImageView mImage;

    GeneralViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mImage = itemView.findViewById(R.id.image);
      mText = itemView.findViewById(R.id.text);
    }

    @NonNull
    public TextView getText()
    {
      return mText;
    }

    @NonNull
    public ImageView getImage()
    {
      return mImage;
    }
  }

  public static class HeaderViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mButton;
    @NonNull
    private final TextView mText;


    HeaderViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mButton = itemView.findViewById(R.id.button);
      mText = itemView.findViewById(R.id.text_message);
    }

    @NonNull
    public TextView getText()
    {
      return mText;
    }

    @NonNull
    public TextView getButton()
    {
      return mButton;
    }

    void setAction(@NonNull HeaderAction action,
                   final boolean showAll)
    {
      mButton.setText(showAll
                      ? R.string.bookmark_lists_show_all
                      : R.string.bookmark_lists_hide_all);
      mButton.setOnClickListener(new ToggleShowAllClickListener(action, showAll));
    }

    void setAction(@NonNull HeaderActionChildCategories action,
                   final boolean showAll)
    {
      mButton.setText(showAll
                      ? R.string.bookmark_lists_show_all
                      : R.string.bookmark_lists_hide_all);
      mButton.setOnClickListener(new ToggleShowAllChildCategoryClickListener(
          action, showAll));
    }

    public interface HeaderAction
    {
      void onHideAll();

      void onShowAll();
    }

    public interface HeaderActionChildCategories
    {
      void onHideAll();

      void onShowAll();
    }

    private static class ToggleShowAllChildCategoryClickListener implements View.OnClickListener
    {
      private final HeaderActionChildCategories mAction;
      private final boolean mShowAll;

      ToggleShowAllChildCategoryClickListener(@NonNull HeaderActionChildCategories action,
                                              boolean showAll)
      {
        mAction = action;
        mShowAll = showAll;
      }

      @Override
      public void onClick(View view)
      {
        if (mShowAll)
          mAction.onShowAll();
        else
          mAction.onHideAll();
      }
    }

    private static class ToggleShowAllClickListener implements View.OnClickListener
    {
      private final HeaderAction mAction;
      private final boolean mShowAll;

      ToggleShowAllClickListener(@NonNull HeaderAction action, boolean showAll)
      {
        mAction = action;
        mShowAll = showAll;
      }

      @Override
      public void onClick(View view)
      {
        if (mShowAll)
          mAction.onShowAll();
        else
          mAction.onHideAll();
      }
    }
  }

  static class CollectionViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final View mView;
    @NonNull
    private final TextView mName;
    @NonNull
    private final CheckBox mVisibilityMarker;
    @NonNull
    private final TextView mSize;
    @Nullable
    private BookmarkCategory mEntity;

    CollectionViewHolder(@NonNull View root)
    {
      super(root);
      mView = root;
      mName = root.findViewById(R.id.name);
      mVisibilityMarker = root.findViewById(R.id.checkbox);
      mSize = root.findViewById(R.id.size);
    }

    void setOnClickListener(@Nullable OnItemClickListener<BookmarkCategory> listener)
    {
      mView.setOnClickListener(v -> {
        if (listener != null && mEntity != null)
          listener.onItemClick(v, mEntity);
      });
    }

    void setVisibilityState(boolean visible)
    {
      mVisibilityMarker.setChecked(visible);
    }

    void setVisibilityListener(@Nullable View.OnClickListener listener)
    {
      mVisibilityMarker.setOnClickListener(listener);
    }

    void setName(@NonNull String name)
    {
      mName.setText(name);
    }

    void setSize(@PluralsRes int phrase, int size)
    {
      mSize.setText(mSize.getResources().getQuantityString(phrase, size, size));
    }

    void setEntity(@NonNull BookmarkCategory entity)
    {
      mEntity = entity;
    }

    @NonNull
    public BookmarkCategory getEntity()
    {
      if (mEntity == null)
        throw new AssertionError("BookmarkCategory is null");
      return mEntity;
    }
  }

  static class CategoryViewHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final TextView mName;
    @NonNull
    CheckBox mVisibilityMarker;
    @NonNull
    ImageView mMoreButton;
    @NonNull
    TextView mSize;
    @Nullable
    private BookmarkCategory mEntity;

    CategoryViewHolder(@NonNull View root)
    {
      super(root);
      mName = root.findViewById(R.id.name);
      mVisibilityMarker = root.findViewById(R.id.checkbox);
      mMoreButton = root.findViewById(R.id.more);
      int left = root.getResources().getDimensionPixelOffset(R.dimen.margin_half_plus);
      int right = root.getResources().getDimensionPixelOffset(R.dimen.margin_base_plus);
      UiUtils.expandTouchAreaForView(mVisibilityMarker, 0, left, 0, right);
      mSize = root.findViewById(R.id.size);
    }

    void setVisibilityState(boolean visible)
    {
      mVisibilityMarker.setChecked(visible);
    }

    void setVisibilityListener(@Nullable View.OnClickListener listener)
    {
      mVisibilityMarker.setOnClickListener(listener);
    }

    void setMoreButtonClickListener(@Nullable View.OnClickListener listener)
    {
      mMoreButton.setOnClickListener(listener);
    }

    void setName(@NonNull String name)
    {
      mName.setText(name);
    }

    void setSize(@PluralsRes int phrase, int size)
    {
      mSize.setText(mSize.getResources().getQuantityString(phrase, size, size));
    }

    void setCategory(@NonNull BookmarkCategory entity)
    {
      mEntity = entity;
    }

    @NonNull
    public BookmarkCategory getEntity()
    {
      if (mEntity == null)
        throw new AssertionError("BookmarkCategory is null");
      return mEntity;
    }
  }

  static abstract class BaseBookmarkHolder extends RecyclerView.ViewHolder
  {
    @NonNull
    private final View mView;

    BaseBookmarkHolder(@NonNull View itemView)
    {
      super(itemView);
      mView = itemView;
    }

    abstract void bind(@NonNull SectionPosition position,
                       @NonNull BookmarkListAdapter.SectionsDataSource sectionsDataSource);

    void setOnClickListener(@Nullable RecyclerClickListener listener)
    {
      mView.setOnClickListener(v -> {
        if (listener != null)
          listener.onItemClick(v, getAdapterPosition());
      });
    }

    void setOnLongClickListener(@Nullable RecyclerLongClickListener listener)
    {
      mView.setOnLongClickListener(v -> {
        if (listener != null)
          listener.onLongItemClick(v, getAdapterPosition());
        return true;
      });
    }
  }

  static class BookmarkViewHolder extends BaseBookmarkHolder
  {
    @NonNull
    private final ImageView mIcon;
    @NonNull
    private final TextView mName;
    @NonNull
    private final TextView mDistance;

    BookmarkViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mIcon = itemView.findViewById(R.id.iv__bookmark_color);
      mName = itemView.findViewById(R.id.tv__bookmark_name);
      mDistance = itemView.findViewById(R.id.tv__bookmark_distance);
    }

    @Override
    void bind(@NonNull SectionPosition position,
              @NonNull BookmarkListAdapter.SectionsDataSource sectionsDataSource)
    {
      final long bookmarkId = sectionsDataSource.getBookmarkId(position);
      BookmarkInfo bookmark = new BookmarkInfo(sectionsDataSource.getCategory().getId(),
                                               bookmarkId);
      mName.setText(bookmark.getName());
      final Location loc = LocationHelper.INSTANCE.getSavedLocation();

      String distanceValue = loc == null ? "" : bookmark.getDistance(loc.getLatitude(),
                                                                     loc.getLongitude(), 0.0);
      String separator = "";
      if (!distanceValue.isEmpty() && !bookmark.getFeatureType().isEmpty())
        separator = " • ";
      String subtitleValue = distanceValue.concat(separator).concat(bookmark.getFeatureType());
      mDistance.setText(subtitleValue);
      UiUtils.hideIf(TextUtils.isEmpty(subtitleValue), mDistance);

      mIcon.setImageResource(bookmark.getIcon().getResId());
      Drawable circle = Graphics.drawCircleAndImage(bookmark.getIcon().argb(),
                                                    R.dimen.track_circle_size,
                                                    bookmark.getIcon().getResId(),
                                                    R.dimen.bookmark_icon_size,
                                                    mIcon.getContext());
      mIcon.setImageDrawable(circle);
    }
  }

  static class TrackViewHolder extends BaseBookmarkHolder
  {
    @NonNull
    private final ImageView mIcon;
    @NonNull
    private final TextView mName;
    @NonNull
    private final TextView mDistance;

    TrackViewHolder(@NonNull View itemView)
    {
      super(itemView);
      mIcon = itemView.findViewById(R.id.iv__bookmark_color);
      mName = itemView.findViewById(R.id.tv__bookmark_name);
      mDistance = itemView.findViewById(R.id.tv__bookmark_distance);
    }

    @Override
    void bind(@NonNull SectionPosition position,
              @NonNull BookmarkListAdapter.SectionsDataSource sectionsDataSource)
    {
      final long trackId = sectionsDataSource.getTrackId(position);
      Track track = BookmarkManager.INSTANCE.getTrack(trackId);
      mName.setText(track.getName());
      mDistance.setText(new StringBuilder().append(mDistance.getContext()
                                                            .getString(R.string.length))
                                           .append(" ")
                                           .append(track.getLengthString())
                                           .toString());
      Drawable circle = Graphics.drawCircle(track.getColor(), R.dimen.track_circle_size,
                                            mIcon.getContext().getResources());
      mIcon.setImageDrawable(circle);
    }
  }

  public static class SectionViewHolder extends BaseBookmarkHolder
  {
    @NonNull
    private final TextView mView;

    SectionViewHolder(@NonNull TextView itemView)
    {
      super(itemView);
      mView = itemView;
    }

    @Override
    void bind(@NonNull SectionPosition position,
              @NonNull BookmarkListAdapter.SectionsDataSource sectionsDataSource)
    {
      mView.setText(sectionsDataSource.getTitle(position.getSectionIndex(), mView.getResources()));
    }
  }

  static class DescriptionViewHolder extends BaseBookmarkHolder
  {
    static final float SPACING_MULTIPLE = 1.0f;
    static final float SPACING_ADD = 0.0f;
    @NonNull
    private final TextView mTitle;
    @NonNull
    private final TextView mDescText;

    DescriptionViewHolder(@NonNull View itemView, @NonNull BookmarkCategory category)
    {
      super(itemView);
      mDescText = itemView.findViewById(R.id.text);
      mTitle = itemView.findViewById(R.id.title);
    }

    @Override
    void bind(@NonNull SectionPosition position,
              @NonNull BookmarkListAdapter.SectionsDataSource sectionsDataSource)
    {
      mTitle.setText(sectionsDataSource.getCategory().getName());
      bindDescriptionIfEmpty(sectionsDataSource.getCategory());
    }

    private void bindDescriptionIfEmpty(@NonNull BookmarkCategory category)
    {
      if (TextUtils.isEmpty(mDescText.getText()))
      {
        String desc = TextUtils.isEmpty(category.getAnnotation())
                      ? category.getDescription()
                      : category.getAnnotation();

        Spanned spannedDesc = Html.fromHtml(desc);
        mDescText.setText(spannedDesc);
      }
    }
  }
}
