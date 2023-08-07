package app.organicmaps.bookmarks.data;

import app.organicmaps.util.Distance;

public class Track
{
  private final long mTrackId;
  private final long mCategoryId;
  private final String mName;
  private final Distance mLength;
  private final int mColor;

  Track(long trackId, long categoryId, String name, Distance length, int color)
  {
    mTrackId = trackId;
    mCategoryId = categoryId;
    mName = name;
    mLength = length;
    mColor = color;
  }

  public String getName() { return mName; }

  public Distance getLength() { return mLength;}

  public int getColor() { return mColor; }

  public long getTrackId() { return mTrackId; }

  public long getCategoryId() { return mCategoryId; }
}
