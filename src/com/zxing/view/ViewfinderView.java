/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zxing.view;

import java.util.Collection;
import java.util.HashSet;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.ericssonlabs.R;
import com.google.zxing.ResultPoint;
import com.zxing.camera.CameraManager;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 */
public final class ViewfinderView extends View {

	private static final int[] SCANNER_ALPHA = { 0, 64, 128, 192, 255, 192, 128, 64 };
	private static final long ANIMATION_DELAY = 100L;
	private static final int OPAQUE = 0xFF;
	private static final int CURRENT_POINT_OPACITY = 0xA0;
	private final Paint paint;
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final int frameColor;
	private final int laserColor;
	private final int resultPointColor;
	private int scannerAlpha;

	private int i = 0;
	private Rect mRect;
	private Drawable lineDrawable;// 中间的扫描线
	private GradientDrawable mDrawable;
	private Collection<ResultPoint> possibleResultPoints;
	private Collection<ResultPoint> lastPossibleResultPoints;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		frameColor = resources.getColor(R.color.viewfinder_frame);
		laserColor = resources.getColor(R.color.viewfinder_laser);
		resultPointColor = resources.getColor(R.color.possible_result_points);
		scannerAlpha = 0;
		possibleResultPoints = new HashSet<ResultPoint>(5);

		mRect = new Rect();
		int left = getResources().getColor(R.color.lightgreen);
		int center = getResources().getColor(R.color.green);
		int right = getResources().getColor(R.color.lightgreen);
		lineDrawable = getResources().getDrawable(R.drawable.zx_code_line);
		mDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] { left, left, center, right, right });
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();
		if (frame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		// Draw the exterior (i.e. outside the framing rect) darkened
		paint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom, paint);
		canvas.drawRect(frame.right, frame.top, width, frame.bottom, paint);
		canvas.drawRect(0, frame.bottom, width, height, paint);

		if (resultBitmap != null) {
			// Draw the opaque result bitmap over the scanning rectangle
			paint.setAlpha(CURRENT_POINT_OPACITY);
			canvas.drawBitmap(resultBitmap, null, frame, paint);
		} else {
			// 画方框四角
			paint.setColor(getResources().getColor(R.color.red_f9));
			canvas.drawRect(frame.left, frame.top, frame.left + 60, frame.top + 5, paint);
			canvas.drawRect(frame.left, frame.top, frame.left + 5, frame.top + 60, paint);
			canvas.drawRect(frame.right - 60, frame.top, frame.right, frame.top + 5, paint);
			canvas.drawRect(frame.right - 5, frame.top, frame.right, frame.top + 60, paint);
			canvas.drawRect(frame.left, frame.bottom - 5, frame.left + 60, frame.bottom, paint);
			canvas.drawRect(frame.left, frame.bottom - 60, frame.left + 5, frame.bottom, paint);
			canvas.drawRect(frame.right - 60, frame.bottom - 5, frame.right, frame.bottom, paint);
			canvas.drawRect(frame.right - 5, frame.bottom - 60, frame.right, frame.bottom, paint);
			// 画扫描线
			paint.setColor(getResources().getColor(R.color.red_f9));
			paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
			scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
			if ((i += 5) < frame.bottom - frame.top) {
				mRect.set(frame.left - 6, frame.top + i - 6, frame.right + 6, frame.top + 6 + i);
				lineDrawable.setBounds(mRect);
				lineDrawable.draw(canvas);
				invalidate();
			} else {
				i = 0;
			}
			// Request another update at the animation interval, but only
			// repaint the laser line,
			// not the entire viewfinder mask.
			postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
		}
	}

	public void drawViewfinder() {
		resultBitmap = null;
		invalidate();
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 *
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResultBitmap(Bitmap barcode) {
		resultBitmap = barcode;
		invalidate();
	}

	public void addPossibleResultPoint(ResultPoint point) {
		possibleResultPoints.add(point);
	}

}
