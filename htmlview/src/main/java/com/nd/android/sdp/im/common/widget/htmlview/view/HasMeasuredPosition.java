package com.nd.android.sdp.im.common.widget.htmlview.view;

public interface HasMeasuredPosition {
  int getMeasuredX();
  int getMeasuredY();
  void setMeasuredPosition(int x, int y);
  void setMeasuredX(int x);
  void setMeasuredY(int y);
}
