package app.organicmaps.help;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import app.organicmaps.R;
import app.organicmaps.WebContainerDelegate;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.base.OnBackPressListener;
import app.organicmaps.util.Constants;

public class CopyrightFragment extends BaseMwmFragment implements OnBackPressListener
{
  private WebContainerDelegate mDelegate;

  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
  {
    View root = inflater.inflate(R.layout.fragment_web_view_with_progress, container, false);

    mDelegate = new WebContainerDelegate(root, Constants.Url.COPYRIGHT)
    {
      @Override
      protected void doStartActivity(Intent intent)
      {
        startActivity(intent);
      }
    };

    return root;
  }

  @Override
  public boolean onBackPressed()
  {
    if (!mDelegate.onBackPressed())
    {
      ((HelpActivity) requireActivity()).stackFragment(HelpFragment.class, getString(R.string.help), null);
    }

    return true;
  }
}
