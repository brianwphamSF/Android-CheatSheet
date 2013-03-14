package com.brian.cheatsheet;

import java.io.File;
import java.util.HashMap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.samsung.samm.common.SObjectImage;
import com.samsung.samm.common.SOptionSCanvas;
import com.samsung.spensdk.SCanvasConstants;
import com.samsung.spensdk.SCanvasView;
import com.samsung.spensdk.applistener.FileProcessListener;
import com.samsung.spensdk.applistener.HistoryUpdateListener;
import com.samsung.spensdk.applistener.SCanvasInitializeListener;
import com.samsung.spensdk.applistener.SCanvasModeChangedListener;

/**
 * Basically, the base class for an Android application window is
 * an Activity.  Within the Activity, the window has to have methods
 * that provide functionality to the user.  Examples: onCreate(Bundle bundle)
 * that loads up functionality for the Activity, onBackPressed() when the
 * back button is pressed, etc....
 */
public class CheatSheetUI extends Activity {

	private final String TAG = "Cheat Sheet";

	// ==============================
	// Application Identifier Setting
	// "SDK Sample Application 1.0"
	// ==============================
	private final String APPLICATION_ID_NAME = "Cheat Sheet Application";
	private final int APPLICATION_ID_VERSION_MAJOR = 2;
	private final int APPLICATION_ID_VERSION_MINOR = 2;
	private final String APPLICATION_ID_VERSION_PATCHNAME = "Debug";
	
	private final int REQUEST_CODE_INSERT_IMAGE_OBJECT = 100;
	private final int REQUEST_CODE_FILE_SELECT = 101;
	
	private final String DEFAULT_SAVE_PATH = "SPenSDK2.2";
	private final String DEFAULT_ATTACH_PATH = "SPenSDK2.2/attach";
	private final String DEFAULT_FILE_EXT = ".png";

	// ==============================
	// Variables
	// ==============================
	Context mContext = null;
	
	private String  mTempAMSFolderPath = null;

	private FrameLayout mLayoutContainer;
	private SCanvasView mSCanvas, nextSCanvas, tempSCanvas;
	private ImageView mPenBtn;
	private ImageView mEraserBtn;
	private ImageView mInsertBtn;
	private ImageView mUndoBtn;
	private ImageView mRedoBtn;
	private ImageView mFlipBtn;
	private ViewFlipper vf;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	
	/**
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 * 
	 * Set up for application launch.
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// User Interface (in this case, we are using an XML file)
		setContentView(R.layout.editor_ui_with_flipper);

		mContext = this;

		// ------------------------------------
		// UI Setting (these are defined by the XML file above)
		// ------------------------------------
		mPenBtn = (ImageView) findViewById(R.id.penBtn);
		mPenBtn.setOnClickListener(penBtnClickListener);
		mEraserBtn = (ImageView) findViewById(R.id.eraseBtn);
		mEraserBtn.setOnClickListener(eraserBtnClickListener);
		mInsertBtn = (ImageView) findViewById(R.id.insertBtn);
		mInsertBtn.setOnClickListener(insertBtnClickListener);

		mUndoBtn = (ImageView) findViewById(R.id.undoBtn);
		mUndoBtn.setOnClickListener(undoNredoBtnClickListener);
		mRedoBtn = (ImageView) findViewById(R.id.redoBtn);
		mRedoBtn.setOnClickListener(undoNredoBtnClickListener);
		
		mFlipBtn = (ImageView) findViewById(R.id.flipBtn);
		mFlipBtn.setOnClickListener(flipBtnClickListener);

		// ------------------------------------
		// Create SCanvasView
		// ------------------------------------
		mLayoutContainer = (FrameLayout) findViewById(R.id.layout_container);
		vf = (ViewFlipper) findViewById(R.id.viewFlipper1);
		
		// Creates two canvases, then our ViewFlipper adds them in with
		// their indices defined.  Could be defined just as vf.addView(View view)
		// but being explicit with vf.addView(View view, int index) makes it better
		// programming practice.
		mSCanvas = new SCanvasView(mContext);
		nextSCanvas = new SCanvasView(mContext);
		// mSCanvas.createSCanvasView((int) 8.5, 11); // note if needed for letter sized actual printing
		vf.addView(mSCanvas, 0);
		vf.addView(nextSCanvas, 1);
		vf.setDisplayedChild(0); // make sure that the first child is set to mSCanvas
		
		loadOnFlip();
	}
	
	/**
	 * loadOnFlip() originally used in onCreate(Bundle bundle) defined above
	 * modified for switches in-between views.
	 */
	public void loadOnFlip() {
		if (vf.getDisplayedChild() == 0) {
			tempSCanvas = mSCanvas;
		}
		if (vf.getDisplayedChild() == 1) {
			tempSCanvas = nextSCanvas;
		}
		// ------------------------------------
		// SettingView Setting
		// Calls the resource map utilities defined in the SPenSDKUtils
		// class.  This was pulled from the SPenSDK, into the application,
		// so it should be okay to use.
		// ------------------------------------
		// Resource Map for Layout & Locale
		HashMap<String, Integer> settingResourceMapInt = SPenSDKUtils
				.getSettingLayoutLocaleResourceMap(true, true, true, true);
		// Resource Map for Custom font path
		HashMap<String, String> settingResourceMapString = SPenSDKUtils
				.getSettingLayoutStringResourceMap(true, true, true, true);
		// Create Setting View
		tempSCanvas.createSettingView(mLayoutContainer, settingResourceMapInt,
			settingResourceMapString);

		// ====================================================================================
		//
		// Set Callback Listener(Interface)
		//
		// ====================================================================================
		// ------------------------------------------------
		// SCanvas Listener
		// ------------------------------------------------
		tempSCanvas.setSCanvasInitializeListener(new SCanvasInitializeListener() {
			@Override
			public void onInitialized() {
				// --------------------------------------------
				// Start SCanvasView/CanvasView Task Here
				// --------------------------------------------
				// Application Identifier Setting
				if (!tempSCanvas.setAppID(APPLICATION_ID_NAME,
						APPLICATION_ID_VERSION_MAJOR,
						APPLICATION_ID_VERSION_MINOR,
						APPLICATION_ID_VERSION_PATCHNAME))
					Toast.makeText(mContext, "Fail to set App ID.",
							Toast.LENGTH_LONG).show();

				// Set Title
				if (!tempSCanvas.setTitle("SPen-SDK Test"))
					Toast.makeText(mContext, "Fail to set Title.",
							Toast.LENGTH_LONG).show();

				// Update button state
				updateModeState();
			}
		});

		// ------------------------------------------------
		// History Change Listener
		// ------------------------------------------------
		tempSCanvas.setHistoryUpdateListener(new HistoryUpdateListener() {
			@Override
			public void onHistoryChanged(boolean undoable, boolean redoable) {
				mUndoBtn.setEnabled(undoable);
				mRedoBtn.setEnabled(redoable);
			}
		});

		// ------------------------------------------------
		// SCanvas Mode Changed Listener
		// ------------------------------------------------
		tempSCanvas.setSCanvasModeChangedListener(new SCanvasModeChangedListener() {

			@Override
			public void onModeChanged(int mode) {
				updateModeState();
			}
		});
		
		//------------------------------------------------
		// File Processing 
		//------------------------------------------------
		tempSCanvas.setFileProcessListener(new FileProcessListener() {
			@Override
			public void onChangeProgress(int nProgress) {
				//Log.i(TAG, "Progress = " + nProgress);
			}

			@Override
			public void onLoadComplete(boolean bLoadResult) {			 
				if(bLoadResult){
					// Show Application Identifier
					String appID = tempSCanvas.getAppID();
					Toast.makeText(CheatSheetUI.this, "Load AMS File("+ appID + ") Success!", Toast.LENGTH_LONG).show();
				}
				else{
					Toast.makeText(CheatSheetUI.this, "Load AMS File Fail!", Toast.LENGTH_LONG).show();				
				}
			}
		});

		mUndoBtn.setEnabled(false);
		mRedoBtn.setEnabled(false);
		mPenBtn.setSelected(true);
		
		// create basic save/road file path
		File sdcard_path = Environment.getExternalStorageDirectory();
		File default_path =  new File(sdcard_path, DEFAULT_SAVE_PATH);
		if(!default_path.exists()){
			if(!default_path.mkdirs()){
				Log.e(TAG, "Default Save Path Creation Error");
				return ;
			}
		}

		// attach file path
		File spen_attach_path =  new File(sdcard_path, DEFAULT_ATTACH_PATH);
		if(!spen_attach_path.exists()){
			if(!spen_attach_path.mkdirs()){
				Log.e(TAG, "Default Attach Path Creation Error");
				return ;
			}
		}

		mTempAMSFolderPath = default_path.getAbsolutePath();
		
		// Caution:
		// Do NOT load file or start animation here because we don't know canvas
		// size here.
		// Start such SCanvasView Task at onInitialized() of
		// SCanvasInitializeListener
	}
	
	/**
	 * Displays the menu when the Menu button is pressed.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		//return super.onCreateOptionsMenu(menu);
		super.onCreateOptionsMenu(menu);
		MenuInflater blowUp = getMenuInflater();
		blowUp.inflate(R.menu.menu, menu);
		return true;
	}
	
	/**
	 * Functionality for each portion of the menu.  Can be
	 * added/deleted for each menu item (hence its class name)
	 * associated with the res/menu/menu.xml file.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.aboutUs:
			// This intent can be changed later on.
			Intent i = new Intent(this, AboutUs.class);
			startActivity(i);

			break;
		case R.id.load:
			callGalleryForInputImage(REQUEST_CODE_INSERT_IMAGE_OBJECT);
		/*{
			Intent intent = new Intent(this, ToolListActivity.class);
			String [] exts = new String [] { "jpg", "png", "ams" }; // file extension 			
			intent.putExtra(ToolListActivity.EXTRA_LIST_PATH, mTempAMSFolderPath);
			intent.putExtra(ToolListActivity.EXTRA_FILE_EXT_ARRAY, exts);
			intent.putExtra(ToolListActivity.EXTRA_SEARCH_ONLY_SAMM_FILE, true);
			startActivityForResult(intent, REQUEST_CODE_FILE_SELECT);
		}*/
		break;
		case R.id.save:
			//-------------------------------
			// layout setting
			//-------------------------------
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.alert_dialog_get_text, null);
			TextView textTitle = (TextView)textEntryView.findViewById(R.id.textTitle);
			textTitle.setText("Enter filename to save (default: *.png)");
			AlertDialog dlg = new AlertDialog.Builder(this)
			.setTitle("Save As")
			.setView(textEntryView)
			.setPositiveButton("Save", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					EditText et = (EditText)textEntryView.findViewById(R.id.text);
					String strFileName = et.getText().toString();

					// check file name length, invalid characters, overwrite, extension, etc.
					if(strFileName==null)
						return; 
					
					if(strFileName.length()<=0){
						Toast.makeText(mContext, "Enter file name to save", Toast.LENGTH_LONG).show();
						return;
					}
					if(!SPenSDKUtils.isValidSaveName(strFileName)) {						
						Toast.makeText(mContext, "Invalid character to save! Save file name : "+ strFileName, Toast.LENGTH_LONG).show();
						return;
					}
					
					int nExtIndex = strFileName.lastIndexOf(".");	
					if(nExtIndex==-1)	
						strFileName += DEFAULT_FILE_EXT;
					else{
						String strExt = strFileName.substring(nExtIndex + 1);
						if(strExt==null)
							strFileName += DEFAULT_FILE_EXT;
						else{
							if(strExt.compareToIgnoreCase("png")!=0 && strExt.compareToIgnoreCase("jpg")!=0){
						strFileName += DEFAULT_FILE_EXT;
							}							
						}							
					}				

					String saveFileName = mTempAMSFolderPath + "/" + strFileName;
					checkSameSaveFileName(saveFileName);	
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked cancel so do some stuff */
				}
			})
			.create();
			dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			dlg.show();
			break;
		case R.id.exit:
			finish();
			break;
		}
		return false;
	}

	/**
	 * Like good programming practice, every time something has
	 * been allocated, must be deallocated in the end.
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Release SCanvasView resources
		if (!tempSCanvas.closeSCanvasView())
			Log.e(TAG, "Fail to close SCanvasView");
	}

	/**
	 * When the back button has been pressed, currently calls the
	 * alertActivityFinish() method, and the name of this Activity is Exit.
	 */
	@Override
	public void onBackPressed() {
		SPenSDKUtils.alertActivityFinish(this, "Exit");
	}

	/**
	 * Our undo and redo functionalities.
	 */
	private OnClickListener undoNredoBtnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (v.equals(mUndoBtn)) {
				tempSCanvas.undo();
			} else if (v.equals(mRedoBtn)) {
				tempSCanvas.redo();
			}
			mUndoBtn.setEnabled(tempSCanvas.isUndoable());
			mRedoBtn.setEnabled(tempSCanvas.isRedoable());
		}
	};
	
	/**
	 * The flip functionality.
	 */
	private OnClickListener flipBtnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			vf.showNext();
			loadOnFlip();
			Log.v(TAG, "canvas=" + tempSCanvas);
		}
	};
	
	/**
	 * The pen functionality.
	 */
	private OnClickListener penBtnClickListener = new OnClickListener() {

		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			// If currently the input mode is the pen, show the setting window.
			// Otherwise, close the setting window and update the mode state.
			if (tempSCanvas.getCanvasMode() == SCanvasConstants.SCANVAS_MODE_INPUT_PEN) {
				tempSCanvas.setSettingViewSizeOption(
						SCanvasConstants.SCANVAS_SETTINGVIEW_PEN,
						SCanvasConstants.SCANVAS_SETTINGVIEW_SIZE_NORMAL);
				tempSCanvas.toggleShowSettingView(SCanvasConstants.SCANVAS_SETTINGVIEW_PEN);
			} else {
				tempSCanvas.setCanvasMode(SCanvasConstants.SCANVAS_MODE_INPUT_PEN);
				tempSCanvas.showSettingView(
						SCanvasConstants.SCANVAS_SETTINGVIEW_PEN, false);
				updateModeState();
			}
		}
		
	};
	
	/**
	 * The eraser functionality.
	 */
	private OnClickListener eraserBtnClickListener = new OnClickListener() {

		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (tempSCanvas.getCanvasMode() == SCanvasConstants.SCANVAS_MODE_INPUT_ERASER) {
				tempSCanvas.setSettingViewSizeOption(
						SCanvasConstants.SCANVAS_SETTINGVIEW_ERASER,
						SCanvasConstants.SCANVAS_SETTINGVIEW_SIZE_NORMAL);
				tempSCanvas.toggleShowSettingView(SCanvasConstants.SCANVAS_SETTINGVIEW_ERASER);
			} else {
				tempSCanvas.setCanvasMode(SCanvasConstants.SCANVAS_MODE_INPUT_ERASER);
				tempSCanvas.showSettingView(
						SCanvasConstants.SCANVAS_SETTINGVIEW_ERASER, false);
				updateModeState();
			}
		}
		
	};
	
	/**
	 * The insert picture functionality.
	 */
	private OnClickListener insertBtnClickListener = new OnClickListener() {

		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if (v.equals(mInsertBtn)) {	
				callGalleryForInputImage(REQUEST_CODE_INSERT_IMAGE_OBJECT);
			}
		}
		
	};
	
	// Call Gallery
	/**
	 * Makes a new intent (can be thought of as a new inactive window)
	 * and once all operations are done, the intent is started using the
	 * startActivityForResult(Intent intent, int requestCode) method.
	 * @param nRequestCode
	 */
	private void callGalleryForInputImage(int nRequestCode){
		try {
			Intent galleryIntent;
			galleryIntent = new Intent(); 
			galleryIntent.setAction(Intent.ACTION_GET_CONTENT);				
			galleryIntent.setType("image/*");
			galleryIntent.setClassName("com.cooliris.media", "com.cooliris.media.Gallery");
			startActivityForResult(galleryIntent, nRequestCode);
		} catch(ActivityNotFoundException e) {
			Intent galleryIntent;
			galleryIntent = new Intent();
			galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
			galleryIntent.setType("image/*");
			startActivityForResult(galleryIntent, nRequestCode);
			e.printStackTrace();
		}		
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	/**
	 * onActivityResult(int requestCode, int resultCode, Intent intent) goes along
	 * with what we want to do (in this case, loading a SAMM file or an image)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		// result must be OK before any operation can be done.
		if (resultCode == RESULT_OK) {
			if(requestCode==REQUEST_CODE_FILE_SELECT){
				Bundle bundle = data.getExtras();
				if(bundle == null)
					return;
				String strFileName = bundle.getString(ToolListActivity.EXTRA_SELECTED_FILE);
				loadSAMMFile(strFileName);						
			}
			else if(requestCode == REQUEST_CODE_INSERT_IMAGE_OBJECT) {    			
				Uri imageFileUri = data.getData();
				String imagePath = SPenSDKUtils.getRealPathFromURI(this, imageFileUri);

				// Check Valid Image File
				if(!SPenSDKUtils.isValidImagePath(imagePath))
				{
					Toast.makeText(this, "Invalid image path or web image", Toast.LENGTH_LONG).show();	
					return;
				}

				RectF rectF = getDefaultRect(imagePath);
				SObjectImage sImageObject = new SObjectImage();
				sImageObject.setRect(rectF);
				sImageObject.setImagePath(imagePath);

				// canvas option setting
				SOptionSCanvas canvasOption = tempSCanvas.getOption();					
				if(canvasOption == null)
					return;
				canvasOption.mSAMMOption.setContentsQuality(PreferencesOfSAMMOption.getPreferenceSaveImageQuality(mContext));
				// option setting
				tempSCanvas.setOption(canvasOption);
				
				if(tempSCanvas.insertSAMMImage(sImageObject, true)){
					Toast.makeText(this, "Insert image file("+ imagePath +") Success!", Toast.LENGTH_SHORT).show();	
				}
				else{
					Toast.makeText(this, "Insert image file("+ imagePath +") Fail!", Toast.LENGTH_LONG).show();    				
				}
			}
		}	
	}

	// Update tool button for the pen and/or eraser
	private void updateModeState() {
		int nCurMode = tempSCanvas.getCanvasMode();
		mPenBtn.setSelected(nCurMode == SCanvasConstants.SCANVAS_MODE_INPUT_PEN);
		mEraserBtn.setSelected(nCurMode == SCanvasConstants.SCANVAS_MODE_INPUT_ERASER);
	}
	
	/**
	 * method takes a SAMM file
	 * @param strFileName
	 * @return
	 */
	boolean loadSAMMFile(String strFileName){
		if(tempSCanvas.isAnimationMode()){
			// It must be not animation mode.
		}
		else {
			// set progress dialog
			tempSCanvas.setProgressDialogSetting("Loading", "Please wait while loading...", ProgressDialog.STYLE_HORIZONTAL, false);

			// canvas option setting
			SOptionSCanvas canvasOption = tempSCanvas.getOption();					
			if(canvasOption == null)
				return false;
			canvasOption.mSAMMOption.setConvertCanvasSizeOption(PreferencesOfSAMMOption.getPreferenceLoadCanvasSize(mContext));
			canvasOption.mSAMMOption.setConvertCanvasHorizontalAlignOption(PreferencesOfSAMMOption.getPreferenceLoadCanvasHAlign(mContext));
			canvasOption.mSAMMOption.setConvertCanvasVerticalAlignOption(PreferencesOfSAMMOption.getPreferenceLoadCanvasVAlign(mContext));
			// option setting
			tempSCanvas.setOption(canvasOption);					
			
			// show progress for loading data
			if(tempSCanvas.loadSAMMFile(strFileName, true, true, true)){
				// Loading Result can be get by callback function
			}
			else{
				Toast.makeText(this, "Load AMS File("+ strFileName +") Fail!", Toast.LENGTH_LONG).show();
				return false;
			}
		}
		return true;
	}
	
	/**
	 * a check to determine if the user wants to overwrite a file or not.
	 * @param saveFileName
	 */
	private void checkSameSaveFileName(final String saveFileName) {	

		File fSaveFile = new File(saveFileName);
		if(fSaveFile.exists())
		{
			AlertDialog dlg = new AlertDialog.Builder(this)
			.setTitle("Same file name exists! Overwrite?")		
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {				
					// canvas option setting
					SOptionSCanvas canvasOption = new SOptionSCanvas();
					// medium size : to reduce saving time 
					canvasOption.mSAMMOption.setSaveImageSize(PreferencesOfSAMMOption.getPreferenceSaveImageSize(mContext));
					//					canvasOption.mSaveOption.setSaveImageSize(SOptionSAMM.SAMM_SAVE_OPTION_MEDIUM_SIZE);
					// valid only to save jpg
					// canvasOption.mSAMMOption.setJPGImageQuality(100);
					// Cropping option 
					canvasOption.mSAMMOption.setSaveImageLeftCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageHorizontalCrop(mContext));
					canvasOption.mSAMMOption.setSaveImageRightCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageHorizontalCrop(mContext));
					canvasOption.mSAMMOption.setSaveImageTopCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageVerticalCrop(mContext));
					canvasOption.mSAMMOption.setSaveImageBottomCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageVerticalCrop(mContext));
					canvasOption.mSAMMOption.setSaveContentsCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveContentsCrop(mContext));
					// content quality minimum 
					canvasOption.mSAMMOption.setContentsQuality(PreferencesOfSAMMOption.getPreferenceSaveImageQuality(mContext));
					// canvasOption.mSAMMOption.setContentsQuality(SOptionSAMM.SAMM_CONTETNS_QUALITY_MINIMUM);
					// with background(image, color) set
					canvasOption.mSAMMOption.setSaveOnlyForegroundImage(PreferencesOfSAMMOption.getPreferenceSaveOnlyForegroundImage(mContext));
					// canvasOption.mSAMMOption.setSaveOnlyForegroundImage(false);	// with background(image, color) set 
					// canvasOption.mSAMMOption.setSaveOnlyForegroundImage(true);	// no background
					// Create new image file to save
					canvasOption.mSAMMOption.setCreateNewImageFile(PreferencesOfSAMMOption.getPreferenceSaveCreateNewImageFile(mContext));
					canvasOption.mSAMMOption.setEncodeForegroundImage(PreferencesOfSAMMOption.getPreferenceEncodeForegroundImageFile(mContext));
					canvasOption.mSAMMOption.setEncodeThumbnailImage(PreferencesOfSAMMOption.getPreferenceEncodeThumbnailImageFile(mContext));
					// option setting
					tempSCanvas.setOption(canvasOption);					
					saveSAMMFile(saveFileName, true);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked cancel so do some stuff */
				}
			})
			.create();
			dlg.show();
		}
		else {
			// canvas option setting
			SOptionSCanvas canvasOption = new SOptionSCanvas();
			// Cropping option 
			canvasOption.mSAMMOption.setSaveImageLeftCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageHorizontalCrop(mContext));
			canvasOption.mSAMMOption.setSaveImageRightCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageHorizontalCrop(mContext));
			canvasOption.mSAMMOption.setSaveImageTopCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageVerticalCrop(mContext));
			canvasOption.mSAMMOption.setSaveImageBottomCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveImageVerticalCrop(mContext));
			canvasOption.mSAMMOption.setSaveContentsCroppingOption(PreferencesOfSAMMOption.getPreferenceSaveContentsCrop(mContext));
			// medium size : to reduce saving time 
			canvasOption.mSAMMOption.setSaveImageSize(PreferencesOfSAMMOption.getPreferenceSaveImageSize(mContext));
			// canvasOption.mSAMMOption.setSaveImageSize(SOptionSAMM.SAMM_SAVE_OPTION_MEDIUM_SIZE);
			// valid only to save jpg
			// canvasOption.mSAMMOption.setJPGImageQuality(100);
			// content quality minimum 
			canvasOption.mSAMMOption.setContentsQuality(PreferencesOfSAMMOption.getPreferenceSaveImageQuality(mContext));
			// canvasOption.mSAMMOption.setContentsQuality(SOptionSAMM.SAMM_CONTETNS_QUALITY_MINIMUM);
			// save with background setting
			canvasOption.mSAMMOption.setSaveOnlyForegroundImage(PreferencesOfSAMMOption.getPreferenceSaveOnlyForegroundImage(mContext));	// with background(image, color) set
			// canvasOption.mSAMMOption.setSaveOnlyForegroundImage(false);	// with background(image, color) set 
			// canvasOption.mSAMMOption.setSaveOnlyForegroundImage(true);	// no background
			canvasOption.mSAMMOption.setCreateNewImageFile(PreferencesOfSAMMOption.getPreferenceSaveCreateNewImageFile(mContext));	// with background(image, color) set
			canvasOption.mSAMMOption.setEncodeForegroundImage(PreferencesOfSAMMOption.getPreferenceEncodeForegroundImageFile(mContext));
			canvasOption.mSAMMOption.setEncodeThumbnailImage(PreferencesOfSAMMOption.getPreferenceEncodeThumbnailImageFile(mContext));
			// option setting
			tempSCanvas.setOption(canvasOption);					
			saveSAMMFile(saveFileName, true);			
		}
	}
	
	boolean saveSAMMFile(String strFileName, boolean bShowSuccessLog){		
		if(tempSCanvas.saveSAMMFile(strFileName)){
			if(bShowSuccessLog){
				Toast.makeText(this, "Save AMS File("+ strFileName +") Success!", Toast.LENGTH_LONG).show();
			}
			return true;
		}
		else{
			Toast.makeText(this, "Save AMS File("+ strFileName +") Fail!", Toast.LENGTH_LONG).show();
			return false;
		}
	}
	
	RectF getDefaultRect(String strImagePath){
		// Rect Region : Consider image real size
		BitmapFactory.Options opts = SPenSDKUtils.getBitmapSize(strImagePath);
		int nImageWidth = opts.outWidth;
		int nImageHeight = opts.outHeight;
		int nScreenWidth = tempSCanvas.getWidth();
		int nScreenHeight = tempSCanvas.getHeight();    			
		int nBoxRadius = (nScreenWidth>nScreenHeight) ? nScreenHeight/4 : nScreenWidth/4;
		int nCenterX = nScreenWidth/2;
		int nCenterY = nScreenHeight/2;
		if(nImageWidth > nImageHeight)
			return new RectF(nCenterX-nBoxRadius,nCenterY-(nBoxRadius*nImageHeight/nImageWidth),nCenterX+nBoxRadius,nCenterY+(nBoxRadius*nImageHeight/nImageWidth));
		else
			return new RectF(nCenterX-(nBoxRadius*nImageWidth/nImageHeight),nCenterY-nBoxRadius,nCenterX+(nBoxRadius*nImageWidth/nImageHeight),nCenterY+nBoxRadius);
	}
}
