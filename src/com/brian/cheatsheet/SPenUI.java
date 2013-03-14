package com.brian.cheatsheet;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.samsung.spensdk.SCanvasConstants;
import com.samsung.spensdk.SCanvasView;
import com.samsung.spensdk.applistener.HistoryUpdateListener;
import com.samsung.spensdk.applistener.SCanvasInitializeListener;
import com.samsung.spensdk.applistener.SCanvasModeChangedListener;

public class SPenUI extends Activity {

	private final String TAG = "Cheat Sheet";

	// ==============================
	// Application Identifier Setting
	// "SDK Sample Application 1.0"
	// ==============================
	private final String APPLICATION_ID_NAME = "Cheat Sheet Application";
	private final int APPLICATION_ID_VERSION_MAJOR = 2;
	private final int APPLICATION_ID_VERSION_MINOR = 2;
	private final String APPLICATION_ID_VERSION_PATCHNAME = "Debug";

	// ==============================
	// Variables
	// ==============================
	Context mContext = null;

	private FrameLayout mLayoutContainer;
	private RelativeLayout mCanvasContainer;
	private SCanvasView mSCanvas;
	private ImageView mPenBtn;
	private ImageView mEraserBtn;
	private ImageView mTextBtn;
	private ImageView mUndoBtn;
	private ImageView mRedoBtn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.editor_basic_ui);
		
		mContext = this;

		// ------------------------------------
		// UI Setting
		// ------------------------------------
		mPenBtn = (ImageView) findViewById(R.id.penBtn);
		mPenBtn.setOnClickListener(mBtnClickListener);
		mEraserBtn = (ImageView) findViewById(R.id.eraseBtn);
		mEraserBtn.setOnClickListener(mBtnClickListener);
		mTextBtn = (ImageView) findViewById(R.id.textBtn);
		mTextBtn.setOnClickListener(mBtnClickListener);

		mUndoBtn = (ImageView) findViewById(R.id.undoBtn);
		mUndoBtn.setOnClickListener(undoNredoBtnClickListener);
		mRedoBtn = (ImageView) findViewById(R.id.redoBtn);
		mRedoBtn.setOnClickListener(undoNredoBtnClickListener);

		// ------------------------------------
		// Create SCanvasView
		// ------------------------------------
		mLayoutContainer = (FrameLayout) findViewById(R.id.layout_container);
		mCanvasContainer = (RelativeLayout) findViewById(R.id.canvas_container);

		mSCanvas = new SCanvasView(mContext);
		// mSCanvas.createSCanvasView((int) 8.5, 11); // note if needed for
		// actual printing
		mCanvasContainer.addView(mSCanvas);

		// ------------------------------------
		// SettingView Setting
		// ------------------------------------
		// Resource Map for Layout & Locale
		HashMap<String, Integer> settingResourceMapInt = SPenSDKUtils
				.getSettingLayoutLocaleResourceMap(true, true, true, true);
		// Resource Map for Custom font path
		HashMap<String, String> settingResourceMapString = SPenSDKUtils
				.getSettingLayoutStringResourceMap(true, true, true, true);
		// Create Setting View
		mSCanvas.createSettingView(mLayoutContainer, settingResourceMapInt,
				settingResourceMapString);

		// ====================================================================================
		//
		// Set Callback Listener(Interface)
		//
		// ====================================================================================
		// ------------------------------------------------
		// SCanvas Listener
		// ------------------------------------------------
		mSCanvas.setSCanvasInitializeListener(new SCanvasInitializeListener() {
			@Override
			public void onInitialized() {
				// --------------------------------------------
				// Start SCanvasView/CanvasView Task Here
				// --------------------------------------------
				// Application Identifier Setting
				if (!mSCanvas.setAppID(APPLICATION_ID_NAME,
						APPLICATION_ID_VERSION_MAJOR,
						APPLICATION_ID_VERSION_MINOR,
						APPLICATION_ID_VERSION_PATCHNAME))
					Toast.makeText(mContext, "Fail to set App ID.",
							Toast.LENGTH_LONG).show();

				// Set Title
				if (!mSCanvas.setTitle("SPen-SDK Test"))
					Toast.makeText(mContext, "Fail to set Title.",
							Toast.LENGTH_LONG).show();

				// Update button state
				updateModeState();
			}
		});

		// ------------------------------------------------
		// History Change Listener
		// ------------------------------------------------
		mSCanvas.setHistoryUpdateListener(new HistoryUpdateListener() {
			@Override
			public void onHistoryChanged(boolean undoable, boolean redoable) {
				mUndoBtn.setEnabled(undoable);
				mRedoBtn.setEnabled(redoable);
			}
		});

		// ------------------------------------------------
		// SCanvas Mode Changed Listener
		// ------------------------------------------------
		mSCanvas.setSCanvasModeChangedListener(new SCanvasModeChangedListener() {

			@Override
			public void onModeChanged(int mode) {
				updateModeState();
			}
		});

		mUndoBtn.setEnabled(false);
		mRedoBtn.setEnabled(false);
		mPenBtn.setSelected(true);

		// Caution:
		// Do NOT load file or start animation here because we don't know canvas
		// size here.
		// Start such SCanvasView Task at onInitialized() of
		// SCanvasInitializeListener
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Release SCanvasView resources
		if (!mSCanvas.closeSCanvasView())
			Log.e(TAG, "Fail to close SCanvasView");
	}

	@Override
	public void onBackPressed() {
		SPenSDKUtils.alertActivityFinish(this, "Exit");
	}

	private OnClickListener undoNredoBtnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.equals(mUndoBtn)) {
				mSCanvas.undo();
			} else if (v.equals(mRedoBtn)) {
				mSCanvas.redo();
			}
			mUndoBtn.setEnabled(mSCanvas.isUndoable());
			mRedoBtn.setEnabled(mSCanvas.isRedoable());
		}
	};

	OnClickListener mBtnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int nBtnID = v.getId();
			// If the mode is not changed, open the setting view. If the mode is
			// same, close the setting view.
			if (nBtnID == mPenBtn.getId()) {
				if (mSCanvas.getCanvasMode() == SCanvasConstants.SCANVAS_MODE_INPUT_PEN) {
					mSCanvas.setSettingViewSizeOption(
							SCanvasConstants.SCANVAS_SETTINGVIEW_PEN,
							SCanvasConstants.SCANVAS_SETTINGVIEW_SIZE_NORMAL);
					mSCanvas.toggleShowSettingView(SCanvasConstants.SCANVAS_SETTINGVIEW_PEN);
				} else {
					mSCanvas.setCanvasMode(SCanvasConstants.SCANVAS_MODE_INPUT_PEN);
					mSCanvas.showSettingView(
							SCanvasConstants.SCANVAS_SETTINGVIEW_PEN, false);
					updateModeState();
				}
			} else if (nBtnID == mEraserBtn.getId()) {
				if (mSCanvas.getCanvasMode() == SCanvasConstants.SCANVAS_MODE_INPUT_ERASER) {
					mSCanvas.setSettingViewSizeOption(
							SCanvasConstants.SCANVAS_SETTINGVIEW_ERASER,
							SCanvasConstants.SCANVAS_SETTINGVIEW_SIZE_NORMAL);
					mSCanvas.toggleShowSettingView(SCanvasConstants.SCANVAS_SETTINGVIEW_ERASER);
				} else {
					mSCanvas.setCanvasMode(SCanvasConstants.SCANVAS_MODE_INPUT_ERASER);
					mSCanvas.showSettingView(
							SCanvasConstants.SCANVAS_SETTINGVIEW_ERASER, false);
					updateModeState();
				}
			} else if (nBtnID == mTextBtn.getId()) {
				if (mSCanvas.getCanvasMode() == SCanvasConstants.SCANVAS_MODE_INPUT_TEXT) {
					mSCanvas.setSettingViewSizeOption(
							SCanvasConstants.SCANVAS_SETTINGVIEW_TEXT,
							SCanvasConstants.SCANVAS_SETTINGVIEW_SIZE_NORMAL);
					mSCanvas.toggleShowSettingView(SCanvasConstants.SCANVAS_SETTINGVIEW_TEXT);
				} else {
					mSCanvas.setCanvasMode(SCanvasConstants.SCANVAS_MODE_INPUT_TEXT);
					mSCanvas.showSettingView(
							SCanvasConstants.SCANVAS_SETTINGVIEW_TEXT, false);
					updateModeState();
					Toast.makeText(mContext, "Tap Canvas to insert Text",
							Toast.LENGTH_SHORT).show();
				}
			}
		}
	};

	// Update tool button
	private void updateModeState() {
		int nCurMode = mSCanvas.getCanvasMode();
		mPenBtn.setSelected(nCurMode == SCanvasConstants.SCANVAS_MODE_INPUT_PEN);
		mEraserBtn
				.setSelected(nCurMode == SCanvasConstants.SCANVAS_MODE_INPUT_ERASER);
		mTextBtn.setSelected(nCurMode == SCanvasConstants.SCANVAS_MODE_INPUT_TEXT);
	}
}
