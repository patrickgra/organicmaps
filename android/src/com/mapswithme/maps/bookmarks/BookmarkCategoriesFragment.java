package com.mapswithme.maps.bookmarks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.mapswithme.maps.MwmApplication;
import com.mapswithme.maps.R;
import com.mapswithme.maps.adapter.OnItemClickListener;
import com.mapswithme.maps.base.BaseMwmRecyclerFragment;
import com.mapswithme.maps.base.DataChangedListener;
import com.mapswithme.maps.bookmarks.data.BookmarkCategory;
import com.mapswithme.maps.bookmarks.data.BookmarkManager;
import com.mapswithme.maps.bookmarks.data.BookmarkSharingResult;
import com.mapswithme.maps.dialog.DialogUtils;
import com.mapswithme.maps.dialog.EditTextDialogFragment;
import com.mapswithme.maps.widget.PlaceholderView;
import com.mapswithme.maps.widget.recycler.ItemDecoratorFactory;
import com.mapswithme.util.StorageUtils;
import com.mapswithme.util.bottomsheet.MenuBottomSheetFragment;
import com.mapswithme.util.bottomsheet.MenuBottomSheetItem;
import com.mapswithme.util.concurrency.ThreadPool;
import com.mapswithme.util.concurrency.UiThread;
import com.mapswithme.util.log.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BookmarkCategoriesFragment extends BaseMwmRecyclerFragment<BookmarkCategoriesAdapter>
    implements BookmarkManager.BookmarksLoadingListener,
               CategoryListCallback,
               OnItemClickListener<BookmarkCategory>,
               OnItemMoreClickListener<BookmarkCategory>,
               OnItemLongClickListener<BookmarkCategory>,
               BookmarkManager.BookmarksSharingListener,
               MenuBottomSheetFragment.MenuBottomSheetInterface

{
  private static final String TAG = BookmarkCategoriesFragment.class.getSimpleName();

  static final int REQ_CODE_DELETE_CATEGORY = 102;
  static final int REQ_CODE_IMPORT_DIRECTORY = 103;

  private static final int MAX_CATEGORY_NAME_LENGTH = 60;

  public static final String BOOKMARKS_CATEGORIES_MENU_ID = "BOOKMARKS_CATEGORIES_BOTTOM_SHEET";

  @Nullable
  private BookmarkCategory mSelectedCategory;
  @Nullable
  private CategoryEditor mCategoryEditor;

  @SuppressWarnings("NullableProblems")
  @NonNull
  private DataChangedListener mCategoriesAdapterObserver;

  @Override
  @LayoutRes
  protected int getLayoutRes()
  {
    return R.layout.fragment_bookmark_categories;
  }

  @NonNull
  @Override
  protected BookmarkCategoriesAdapter createAdapter()
  {
    List<BookmarkCategory> items = BookmarkManager.INSTANCE.getCategories();
    return new BookmarkCategoriesAdapter(requireContext(), items);
  }

  @CallSuper
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);

    onPrepareControllers(view);
    getAdapter().setOnClickListener(this);
    getAdapter().setOnLongClickListener(this);
    getAdapter().setOnMoreClickListener(this);
    getAdapter().setCategoryListCallback(this);

    RecyclerView rw = getRecyclerView();
    if (rw == null) return;

    rw.setNestedScrollingEnabled(false);
    RecyclerView.ItemDecoration decor = ItemDecoratorFactory
        .createDecoratorWithPadding(getContext());
    rw.addItemDecoration(decor);
    mCategoriesAdapterObserver = new CategoriesAdapterObserver(this);
    BookmarkManager.INSTANCE.addCategoriesUpdatesListener(mCategoriesAdapterObserver);
  }

  protected void onPrepareControllers(@NonNull View view)
  {
    // No op
  }

  @Override
  public void onPreparedFileForSharing(@NonNull BookmarkSharingResult result)
  {
    BookmarksSharingHelper.INSTANCE.onPreparedFileForSharing(requireActivity(), result);
  }

  @Override
  public void onStart()
  {
    super.onStart();
    BookmarkManager.INSTANCE.addLoadingListener(this);
    BookmarkManager.INSTANCE.addSharingListener(this);
  }
  
  @Override
  public void onStop()
  {
    super.onStop();
    BookmarkManager.INSTANCE.removeLoadingListener(this);
    BookmarkManager.INSTANCE.removeSharingListener(this);
  }

  @Override
  public void onResume()
  {
    super.onResume();
    getAdapter().notifyDataSetChanged();
  }

  @Override
  public void onPause()
  {
    super.onPause();
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    BookmarkManager.INSTANCE.removeCategoriesUpdatesListener(mCategoriesAdapterObserver);
  }

  protected final void showBottomMenu(@NonNull BookmarkCategory item)
  {
    mSelectedCategory = item;
    MenuBottomSheetFragment.newInstance(BOOKMARKS_CATEGORIES_MENU_ID, item.getName())
            .show(getChildFragmentManager(), BOOKMARKS_CATEGORIES_MENU_ID);
  }

  @Override
  @Nullable
  public ArrayList<MenuBottomSheetItem> getMenuBottomSheetItems(String id)
  {
    ArrayList<MenuBottomSheetItem> items = new ArrayList<>();
    if (mSelectedCategory != null)
    {
      items.add(new MenuBottomSheetItem(
          R.string.list_settings,
          R.drawable.ic_settings,
          () -> onSettingsActionSelected(mSelectedCategory)));
      items.add(new MenuBottomSheetItem(
          mSelectedCategory.isVisible() ? R.string.hide : R.string.show,
          mSelectedCategory.isVisible() ? R.drawable.ic_hide : R.drawable.ic_show,
          () -> onShowActionSelected(mSelectedCategory)));
      items.add(new MenuBottomSheetItem(
          R.string.export_file,
          R.drawable.ic_share,
          () -> onShareActionSelected(mSelectedCategory)));
      // Disallow deleting the last category
      if (getAdapter().getBookmarkCategories().size() > 1)
        items.add(new MenuBottomSheetItem(
            R.string.delete,
            R.drawable.ic_delete,
            () -> onDeleteActionSelected(mSelectedCategory)));
    }
    return items;
  }

  @Override
  protected void setupPlaceholder(@Nullable PlaceholderView placeholder)
  {
    // A placeholder is no needed on this screen.
  }

  @Override
  public void onBookmarksLoadingStarted()
  {
  }

  @Override
  public void onBookmarksLoadingFinished()
  {
    getAdapter().notifyDataSetChanged();
  }

  @Override
  public void onBookmarksFileLoaded(boolean success)
  {
    // Do nothing here.
  }

  @Override
  public void onAddButtonClick()
  {
    mCategoryEditor = BookmarkManager.INSTANCE::createCategory;

    EditTextDialogFragment dialogFragment =
        EditTextDialogFragment.show(getString(R.string.bookmarks_create_new_group),
                                    getString(R.string.bookmarks_new_list_hint),
                                    getString(R.string.bookmark_set_name),
                                    getString(R.string.create),
                                    getString(R.string.cancel),
                                    MAX_CATEGORY_NAME_LENGTH,
                                    this,
                                    new CategoryValidator());
    dialogFragment.setTextSaveListener(this::onSaveText);
  }

  @Override
  public void onImportButtonClick()
  {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

    // Sic: EXTRA_INITIAL_URI doesn't work
    // https://stackoverflow.com/questions/65326605/extra-initial-uri-will-not-work-no-matter-what-i-do
    // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initial);

    // Enable "Show SD card option"
    // http://stackoverflow.com/a/31334967/1615876
    intent.putExtra("android.content.extra.SHOW_ADVANCED", true);

    intent.putExtra(DocumentsContract.EXTRA_EXCLUDE_SELF, true);
    startActivityForResult(intent, REQ_CODE_IMPORT_DIRECTORY);
  }

  @Override
  public void onItemClick(@NonNull View v, @NonNull BookmarkCategory category)
  {
    mSelectedCategory = category;
    BookmarkListActivity.startForResult(this, category);
  }

  private void onShowActionSelected(@NonNull BookmarkCategory category)
  {
    BookmarkManager.INSTANCE.toggleCategoryVisibility(category);
    getAdapter().notifyDataSetChanged();
  }

  protected void onShareActionSelected(@NonNull BookmarkCategory category)
  {
    BookmarksSharingHelper.INSTANCE.prepareBookmarkCategoryForSharing(requireActivity(), category.getId());
  }

  private void onDeleteActionSelected(@NonNull BookmarkCategory category)
  {
    BookmarkManager.INSTANCE.deleteCategory(category.getId());
    getAdapter().notifyDataSetChanged();
  }

  private void onSettingsActionSelected(@NonNull BookmarkCategory category)
  {
    BookmarkCategorySettingsActivity.startForResult(this, category);
  }

  @Override
  public final void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode != Activity.RESULT_OK)
      return;
    switch (requestCode)
    {
    case REQ_CODE_DELETE_CATEGORY:
    {
      onDeleteActionSelected(getSelectedCategory());
      return;
    }
    case REQ_CODE_IMPORT_DIRECTORY:
    {
      if (data == null)
        throw new AssertionError("Data is null");

      final Context context = getActivity();
      final Uri rootUri = data.getData();
      final ProgressDialog dialog = DialogUtils.createModalProgressDialog(context, R.string.wait_several_minutes);
      dialog.show();
      Logger.d(TAG, "Importing bookmarks from " + rootUri);
      MwmApplication app = MwmApplication.from(context);
      final File tempDir = new File(StorageUtils.getTempPath(app));
      final ContentResolver resolver = context.getContentResolver();
      ThreadPool.getStorage().execute(() -> {
        AtomicInteger found = new AtomicInteger(0);
        StorageUtils.listContentProviderFilesRecursively(
            resolver, rootUri, uri -> {
              if (BookmarkManager.INSTANCE.importBookmarksFile(resolver, uri, tempDir))
                found.incrementAndGet();
            });
        UiThread.run(() -> {
          if (dialog.isShowing())
            dialog.dismiss();
          int found_val = found.get();
          String message = context.getResources().getQuantityString(
              R.plurals.bookmarks_detect_message, found_val, found_val);
          Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
      });
      break;
    }
    default:
      throw new AssertionError("Invalid requestCode: " + requestCode);
    }
  }

  @Override
  public void onItemLongClick(@NonNull View v, @NonNull BookmarkCategory category)
  {
    showBottomMenu(category);
  }

  public void onItemMoreClick(@NonNull View v, @NonNull BookmarkCategory category)
  {
    showBottomMenu(category);
  }

  private void onSaveText(@NonNull String text)
  {
    if (mCategoryEditor != null)
      mCategoryEditor.commit(text);

    getAdapter().notifyDataSetChanged();
  }

  @NonNull
  protected BookmarkCategory getSelectedCategory()
  {
    if (mSelectedCategory == null)
      throw new AssertionError("Invalid attempt to use null selected category.");
    return mSelectedCategory;
  }

  interface CategoryEditor
  {
    void commit(@NonNull String newName);
  }

  private static class CategoriesAdapterObserver implements DataChangedListener<BookmarkCategoriesFragment>
  {
    @Nullable
    private BookmarkCategoriesFragment mFragment;

    CategoriesAdapterObserver(@NonNull BookmarkCategoriesFragment fragment)
    {
      mFragment = fragment;
    }

    @Override
    public void attach(@NonNull BookmarkCategoriesFragment object)
    {
      mFragment = object;
    }

    @Override
    public void detach()
    {
      mFragment = null;
    }

    @Override
    public void onChanged()
    {
      if (mFragment == null)
        return;

      mFragment.getAdapter().setItems(BookmarkManager.INSTANCE.getCategories());
    }
  }
}
